import sys, os, json, math, time
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
from PIL import Image
from cells import *
from matte import matte

OUT = '/home/claude/grow/app/src/main/assets/anim'
FIT_W, FIT_H = 480, 600
N_IN = 3              # in-between frames per segment
PRE = 640             # working size for registration

def bleed(rgba):
    """replace the colour of semi-transparent edge pixels by nearby opaque colours (kills grey halos)"""
    a = np.asarray(rgba)
    rgb = a[..., :3].copy(); al = a[..., 3]
    opaque = (al >= 245).astype(np.uint8)
    # thick = opaque pixels that survive an erosion (rope-like thin lines do not)
    thick = cv2.erode(opaque, np.ones((3, 3), np.uint8))
    near_thick = cv2.dilate(thick, np.ones((7, 7), np.uint8))
    bad = (((al < 245) & (near_thick > 0)).astype(np.uint8)) * 255
    if bad.any():
        rgb = cv2.inpaint(rgb, bad, 3, cv2.INPAINT_TELEA)
    return rgb, al

def sift_match(imgA, imgB):
    """scale + translation (no rotation) mapping B->A, estimated on the static parts of the body"""
    sift = cv2.SIFT_create(nfeatures=2500, contrastThreshold=0.015)
    ga = cv2.cvtColor(imgA, cv2.COLOR_RGB2GRAY); gb = cv2.cvtColor(imgB, cv2.COLOR_RGB2GRAY)
    ka, da = sift.detectAndCompute(ga, None); kb, db = sift.detectAndCompute(gb, None)
    if da is None or db is None or len(ka) < 8 or len(kb) < 8: return None, 0
    m = cv2.BFMatcher().knnMatch(db, da, k=2)
    good = [x[0] for x in m if len(x) == 2 and x[0].distance < 0.78 * x[1].distance]
    if len(good) < 6: return None, 0
    src = np.float32([kb[g.queryIdx].pt for g in good]); dst = np.float32([ka[g.trainIdx].pt for g in good])
    rng = np.random.default_rng(1)
    n = len(src)
    i = rng.integers(0, n, 4000); j = rng.integers(0, n, 4000)
    ds = np.linalg.norm(src[i] - src[j], axis=1)
    ok = ds > 25
    i, j, ds = i[ok], j[ok], ds[ok]
    if len(i) == 0: return None, 0
    s_c = np.linalg.norm(dst[i] - dst[j], axis=1) / ds
    keep = (s_c > 0.6) & (s_c < 1.7)
    i, j, s_c = i[keep], j[keep], s_c[keep]
    if len(i) == 0: return None, 0
    t_c = dst[i] - s_c[:, None] * src[i]
    pred = s_c[:, None, None] * src[None, :, :] + t_c[:, None, :]
    err = np.linalg.norm(pred - dst[None, :, :], axis=2)
    cnt = (err < 4.0).sum(axis=1)
    best = int(cnt.argmax())
    inl = err[best] < 4.0
    if inl.sum() < 4: return None, 0
    ms, md = src[inl].mean(axis=0), dst[inl].mean(axis=0)
    sc = float(((src[inl] - ms) * (dst[inl] - md)).sum() / max(1e-6, ((src[inl] - ms) ** 2).sum()))
    t = md - sc * ms
    M = np.array([[sc, 0, t[0]], [0, sc, t[1]]], np.float64)
    return M, int(inl.sum())

def composite(rgba_pre, bg=(150, 150, 150)):
    a = np.asarray(rgba_pre).astype(np.float32)
    al = a[..., 3:4] / 255.0
    return (a[..., :3] * al + np.array(bg, np.float32) * (1 - al)).astype(np.uint8)

def to3(M): return np.vstack([M, [0, 0, 1]]).astype(np.float64)

def prepare(pil_rgb, flip=False):
    rgba = matte(pil_rgb)
    if flip: rgba = rgba.transpose(Image.FLIP_LEFT_RIGHT)
    return rgba

def alpha_bbox(al, thr=20):
    ys, xs = np.where(al > thr)
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1

def head_width(pre):
    """width of the hair blob that sits right above the red polo (pre-res pixels)"""
    a = np.asarray(pre)
    r, g, b, al = [a[..., i].astype(int) for i in range(4)]
    polo = (r > 150) & (g < 70) & (b < 70) & (al > 200)
    ys, xs = np.where(polo)
    if len(ys) == 0: return None
    top = ys.min()
    lum = (r + g + b) / 3
    dark = ((lum < 75) & (al > 200)).astype(np.uint8)
    dark[top + int(0.06 * a.shape[0]):] = 0
    n, lbl, st, _ = cv2.connectedComponentsWithStats(dark, connectivity=8)
    best = None
    for i in range(1, n):
        w_, h_, ar = st[i, cv2.CC_STAT_WIDTH], st[i, cv2.CC_STAT_HEIGHT], st[i, cv2.CC_STAT_AREA]
        if w_ > 2.0 * h_ or h_ > 2.5 * w_ or ar < 200: continue      # skip bars / thin lines
        if best is None or ar > best[1]: best = (float(w_), ar)
    return best[0] if best else None

FALLBACK_CFG = {}

def register(rgbas, cfg=None):
    """rgbas: list of source-res RGBA PIL. returns per-frame 3x3 matrices into the pre-res space of frame 0, plus pre scale"""
    pres, rs = [], []
    for im in rgbas:
        r = PRE / max(im.size); rs.append(r)
        pres.append(im.resize((int(im.width * r), int(im.height * r)), Image.LANCZOS))
    comps = [composite(p) for p in pres]
    mats = [np.eye(3)]; info = [('ref', 999, False)]
    for j in range(1, len(rgbas)):
        best = None
        for flip in (False, True):
            img = comps[j][:, ::-1].copy() if flip else comps[j]
            M, inl = sift_match(comps[0], img)
            if M is not None and (best is None or inl > best[1]): best = (M, inl, flip)
        if best is None or best[1] < 8:
            # fallback: align bottoms and centres of the silhouettes, equal scale
            a0 = np.asarray(pres[0])[..., 3]; aj = np.asarray(pres[j])[..., 3]
            x0, y0, x1, y1 = alpha_bbox(a0); xj, yj, xj1, yj1 = alpha_bbox(aj)
            sc = (y1 - y0) / max(1, (yj1 - yj))
            sc = min(1.25, max(0.8, sc))
            align = 'bottom'
            if cfg and cfg.get('head'):
                h0, hj = head_width(pres[0]), head_width(pres[j])
                if h0 and hj: sc = min(2.5, max(0.4, h0 / hj))
                if cfg.get('manual'): sc = cfg['manual'][j]
                align = cfg.get('align', 'bottom')
            if align == 'top':
                M3 = np.array([[sc, 0, (x0 + x1) / 2 - sc * (xj + xj1) / 2], [0, sc, y0 - sc * yj], [0, 0, 1]])
            else:
                M3 = np.array([[sc, 0, (x0 + x1) / 2 - sc * (xj + xj1) / 2], [0, sc, y1 - sc * yj1], [0, 0, 1]])
            mats.append(M3); info.append(('fallback', best[1] if best else 0, False))
        else:
            M, inl, flip = best
            M3 = to3(M)
            if flip:
                W = pres[j].width
                F = np.array([[-1, 0, W], [0, 1, 0], [0, 0, 1]], np.float64)
                M3 = M3 @ F
            mats.append(M3); info.append(('sift', inl, flip))
    return mats, rs, pres, info

def render_frames(rgbas, mats, rs, pres):
    """warp every frame to the common final canvas (frame-0 pre-res space, cropped to the union bbox, then fit)"""
    # union bbox
    xs, ys = [], []
    for p, M in zip(pres, mats):
        al = np.asarray(p)[..., 3]
        x0, y0, x1, y1 = alpha_bbox(al)
        pts = np.array([[x0, y0, 1], [x1, y0, 1], [x1, y1, 1], [x0, y1, 1]], np.float64).T
        q = M @ pts
        xs += list(q[0]); ys += list(q[1])
    ux0, uy0, ux1, uy1 = min(xs), min(ys), max(xs), max(ys)
    mx = (ux1 - ux0) * 0.03; my = (uy1 - uy0) * 0.03
    ux0 -= mx; ux1 += mx; uy0 -= my; uy1 += my
    bw, bh = ux1 - ux0, uy1 - uy0
    sf = min(FIT_W / bw, FIT_H / bh)
    W, H = int(round(bw * sf)), int(round(bh * sf))
    T = np.array([[sf, 0, -ux0 * sf], [0, sf, -uy0 * sf], [0, 0, 1]], np.float64)
    out = []
    for im, M, r in zip(rgbas, mats, rs):
        P = np.diag([r, r, 1.0])
        tot = T @ M @ P
        rgb, al = bleed(im)
        prem = np.dstack([rgb.astype(np.float32) * (al[..., None] / 255.0), al.astype(np.float32)])
        w = cv2.warpAffine(prem, tot[:2], (W, H), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
        a = np.clip(w[..., 3], 0, 255)
        # crisper edges after the upscale
        an = a / 255.0
        an = np.clip((an - 0.5) * 1.9 + 0.5, 0, 1)
        col = np.where(a[..., None] > 1, w[..., :3] / np.maximum(a[..., None] / 255.0, 1e-3), 0)
        col = np.clip(col, 0, 255)
        out.append(np.dstack([col, an * 255]).astype(np.float32))
    return out, (W, H)

def sharpen(rgb):
    blur = cv2.GaussianBlur(rgb, (0, 0), 1.3)
    return np.clip(rgb * 1.5 - blur * 0.5, 0, 255)

def gray_for_flow(f):
    al = f[..., 3:4] / 255.0
    lum = cv2.cvtColor(np.clip(f[..., :3], 0, 255).astype(np.uint8), cv2.COLOR_RGB2GRAY).astype(np.float32)
    g = lum * al[..., 0] + 150 * (1 - al[..., 0])
    return np.clip(0.6 * g + 0.4 * f[..., 3], 0, 255).astype(np.uint8)

def morph(A, B, n, dis):
    """n in-between frames between RGBA float frames A and B using optical-flow warping"""
    gA, gB = gray_for_flow(A), gray_for_flow(B)
    fab = dis.calc(gA, gB, None); fba = dis.calc(gB, gA, None)
    H, W = gA.shape
    gx, gy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(H, dtype=np.float32))
    pa = np.dstack([A[..., :3] * (A[..., 3:4] / 255.0), A[..., 3:4]])
    pb = np.dstack([B[..., :3] * (B[..., 3:4] / 255.0), B[..., 3:4]])
    res = []
    for i in range(1, n + 1):
        t = i / (n + 1.0)
        wa = cv2.remap(pa, gx + t * fba[..., 0], gy + t * fba[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
        wb = cv2.remap(pb, gx + (1 - t) * fab[..., 0], gy + (1 - t) * fab[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
        m = (1 - t) * wa + t * wb
        a = np.clip(m[..., 3], 0, 255)
        col = np.where(a[..., None] > 1, m[..., :3] / np.maximum(a[..., None] / 255.0, 1e-3), 0)
        a2 = np.clip((a / 255.0 - 0.56) * 2.4 + 0.5, 0, 1) * 255
        res.append(np.dstack([np.clip(col, 0, 255), a2]).astype(np.float32))
    return res

def crossfade(A, B, n):
    out = []
    pa = np.dstack([A[..., :3] * (A[..., 3:4] / 255.0), A[..., 3:4]]); pb = np.dstack([B[..., :3] * (B[..., 3:4] / 255.0), B[..., 3:4]])
    for i in range(1, n + 1):
        t = i / (n + 1.0); m = (1 - t) * pa + t * pb; a = m[..., 3]
        col = np.where(a[..., None] > 1, m[..., :3] / np.maximum(a[..., None] / 255.0, 1e-3), 0)
        out.append(np.dstack([np.clip(col, 0, 255), a]).astype(np.float32))
    return out

def save_webp(f, path, q=80):
    f = f.copy(); f[..., :3] = sharpen(f[..., :3])
    im = Image.fromarray(np.clip(f, 0, 255).astype(np.uint8), 'RGBA')
    im.save(path, 'WEBP', quality=q, method=6, alpha_quality=85)
    return os.path.getsize(path)

def frames_for(key_pils, mode='flow', cfg=None, flips=None):
    rgbas = [prepare(p, flip=bool(flips and flips[i])) for i, p in enumerate(key_pils)]
    mats, rs, pres, info = register(rgbas, cfg)
    F, size = render_frames(rgbas, mats, rs, pres)
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    seq = []
    for i in range(len(F) - 1):
        seq.append(F[i])
        seq += (morph(F[i], F[i + 1], N_IN, dis) if mode == 'flow' else crossfade(F[i], F[i + 1], N_IN))
    seq.append(F[-1])
    return seq, F, size, info
