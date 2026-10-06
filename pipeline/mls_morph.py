import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
from PIL import Image
from kp import detect

# landmark order inside kp.IDX: 0 nose,1 Lsh,2 Rsh,3 Lel,4 Rel,5 Lwr,6 Rwr,7 Lhip,8 Rhip,9 Lkn,10 Rkn,11 Lan,12 Ran,13 Lheel,14 Rheel,15 Lfoot,16 Rfoot
PAIRS = [(1, 2), (3, 4), (5, 6), (7, 8), (9, 10), (11, 12), (13, 14), (15, 16)]
BONES = [(1, 3), (3, 5), (2, 4), (4, 6), (1, 2), (7, 8), (1, 7), (2, 8), (7, 9), (9, 11), (8, 10), (10, 12), (11, 15), (12, 16), (11, 13), (12, 14), (0, 1), (0, 2)]

def to_pil(f):
    return Image.fromarray(np.clip(f, 0, 255).astype(np.uint8), 'RGBA')

def keypoints(frames, minv=0.45):
    ks = []
    for f in frames:
        k = detect(to_pil(f))
        if k is None: return None
        ks.append(k)
    return ks

def fix_swaps(kps):
    """left/right labels can flip between frames: keep the assignment with the smallest total displacement"""
    out = [kps[0].copy()]
    for k in kps[1:]:
        k = k.copy(); prev = out[-1]
        for a, b in PAIRS:
            straight = np.linalg.norm(k[a, :2] - prev[a, :2]) + np.linalg.norm(k[b, :2] - prev[b, :2])
            swapped = np.linalg.norm(k[a, :2] - prev[b, :2]) + np.linalg.norm(k[b, :2] - prev[a, :2])
            if swapped < straight * 0.8: k[[a, b]] = k[[b, a]]
        out.append(k)
    return out

def control_points(pa, pb, minv=0.45):
    """matched control points (source A, source B), landmarks + points along the bones"""
    ok = (pa[:, 2] > minv) & (pb[:, 2] > minv)
    # drop the distal landmark of a bone whose length changes implausibly between the two poses (mis-detections)
    for a, b in ((7, 9), (9, 11), (8, 10), (10, 12), (1, 3), (3, 5), (2, 4), (4, 6)):
        if ok[a] and ok[b]:
            la = np.linalg.norm(pa[a, :2] - pa[b, :2]); lb = np.linalg.norm(pb[a, :2] - pb[b, :2])
            if la > 1 and lb > 1 and not (0.55 <= lb / la <= 1.8): ok[b] = False
    A, B = [], []
    for i in range(len(pa)):
        if ok[i]: A.append(pa[i, :2]); B.append(pb[i, :2])
    for a, b in BONES:
        if ok[a] and ok[b]:
            for s in (0.25, 0.5, 0.75):
                A.append(pa[a, :2] * (1 - s) + pa[b, :2] * s); B.append(pb[a, :2] * (1 - s) + pb[b, :2] * s)
    # torso centre line
    if ok[1] and ok[2] and ok[7] and ok[8]:
        ca = (pa[1, :2] + pa[2, :2] + pa[7, :2] + pa[8, :2]) / 4; cb = (pb[1, :2] + pb[2, :2] + pb[7, :2] + pb[8, :2]) / 4
        A.append(ca); B.append(cb)
    return np.array(A, np.float64), np.array(B, np.float64)

def mls_rigid_map(p, q, W, H, step=6, alpha=1.2, mode='rigid'):
    """for target pixels v find the source position f(v): p = target control points, q = source control points"""
    gx = np.arange(0, W + step, step, dtype=np.float64); gy = np.arange(0, H + step, step, dtype=np.float64)
    X, Y = np.meshgrid(gx, gy)
    v = (X + 1j * Y).ravel()                                  # M
    pc = p[:, 0] + 1j * p[:, 1]; qc = q[:, 0] + 1j * q[:, 1]  # N
    d2 = np.abs(v[:, None] - pc[None, :]) ** 2 + 1e-3
    w = 1.0 / d2 ** alpha
    sw = w.sum(axis=1)
    ps = (w * pc[None, :]).sum(axis=1) / sw
    qs = (w * qc[None, :]).sum(axis=1) / sw
    ph = pc[None, :] - ps[:, None]; qh = qc[None, :] - qs[:, None]
    C = (w * np.conj(ph) * qh).sum(axis=1)
    if mode == 'sim':
        mu = (w * np.abs(ph) ** 2).sum(axis=1)
        k = C / np.maximum(mu, 1e-9)
        mag = np.abs(k); k = np.where(mag > 0, k / np.maximum(mag, 1e-9) * np.clip(mag, 0.55, 1.8), k)
        f = qs + (v - ps) * k
    else:
        rot = C / np.maximum(np.abs(C), 1e-9)
        f = qs + (v - ps) * rot
    fx = f.real.reshape(X.shape).astype(np.float32); fy = f.imag.reshape(X.shape).astype(np.float32)
    mapx = cv2.resize(fx, (W, H), interpolation=cv2.INTER_LINEAR)
    mapy = cv2.resize(fy, (W, H), interpolation=cv2.INTER_LINEAR)
    # resize maps the grid to the full image: correct for the grid step / sampling offset
    sx = (W - 1) / (X.shape[1] - 1) / step * 1.0; sy = (H - 1) / (X.shape[0] - 1) / step * 1.0
    return mapx, mapy

def premult(f):
    return np.dstack([f[..., :3] * (f[..., 3:4] / 255.0), f[..., 3:4]])

def unpremult(m):
    a = np.clip(m[..., 3], 0, 255)
    col = np.where(a[..., None] > 1, m[..., :3] / np.maximum(a[..., None] / 255.0, 1e-3), 0)
    return np.clip(col, 0, 255), a

SOFT = [False]    # soft cross-fade (rope) instead of "nearest pose wins"

def blend(wa, wb, t):
    """overlapping pixels are averaged; where only one pose has the body part, the nearer pose keeps it (no half-transparent ghosts)"""
    avg = (1 - t) * wa + t * wb
    if SOFT[0]: return avg
    pa, pb = wa[..., 3], wb[..., 3]
    near = wa if t < 0.5 else wb
    f = np.clip(np.minimum(pa, pb) / np.maximum(np.maximum(pa, pb), 1.0), 0, 1)
    m = f[..., None] * avg + (1 - f[..., None]) * near
    both = (pa > 140) & (pb > 140)
    if both.any():
        ca, _ = unpremult(wa); cb, _ = unpremult(wb)
        use = both & (np.abs(ca - cb).mean(axis=2) > 45)
        m = np.where(use[..., None], near, m)
    return m

def warp_pair(PA, PB, ca, cb, t, mode, alpha):
    H, W = PA.shape[:2]
    tgt = (1 - t) * ca + t * cb
    mxa, mya = mls_rigid_map(tgt, ca, W, H, alpha=alpha, mode=mode)
    mxb, myb = mls_rigid_map(tgt, cb, W, H, alpha=alpha, mode=mode)
    wa = cv2.remap(PA, mxa, mya, cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    wb = cv2.remap(PB, mxb, myb, cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    return wa, wb

def agreement(wa, wb):
    a = wa[..., 3] > 128; b = wb[..., 3] > 128
    u = (a | b).sum()
    return float((a & b).sum()) / max(1, u)

def n_holes(mask, min_area=30):
    inv = (~mask).astype(np.uint8)
    n, lbl, st, _ = cv2.connectedComponentsWithStats(inv, connectivity=4)
    h, w = mask.shape
    cnt = 0
    for i in range(1, n):
        x, y, cw, ch, ar = st[i]
        if x > 0 and y > 0 and x + cw < w and y + ch < h and ar >= min_area: cnt += 1
    return cnt

def n_components(mask, min_area=60):
    n, lbl, st, _ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), connectivity=8)
    return int((st[1:, cv2.CC_STAT_AREA] >= min_area).sum())

def quality(wa, wb, A, B):
    """agreement of the two warps, penalised when the in-between area / pieces do not look like the two keys"""
    m = blend(wa, wb, 0.5)
    mid = m[..., 3] > 128
    aA = (A[..., 3] > 128).sum(); aB = (B[..., 3] > 128).sum()
    exp = max(1.0, 0.5 * (aA + aB))
    dev = abs(mid.sum() - exp) / exp
    extra = max(0, n_components(mid) - max(n_components(A[..., 3] > 128), n_components(B[..., 3] > 128)))
    return agreement(wa, wb) - 1.5 * dev - 0.15 * extra

def mls_inbetweens(A, B, pa, pb, n, configs=None, anchors=None):
    PA, PB = premult(A), premult(B)
    best = None
    for minv in (0.45, 0.7):
        ca, cb = control_points(pa, pb, minv)
        if len(ca) < 8: continue
        if anchors is not None and len(anchors):
            ca = np.vstack([ca, anchors]); cb = np.vstack([cb, anchors])
        for mode, alpha in (('rigid', 1.2), ('rigid', 2.0), ('rigid', 3.0)):
            wa, wb = warp_pair(PA, PB, ca, cb, 0.5, mode, alpha)
            sc = quality(wa, wb, A, B)
            if best is None or sc > best[0]: best = (sc, mode, alpha, ca, cb)
    if best is None: return None, 0, None
    sc, mode, alpha, ca, cb = best
    out = []
    for i in range(1, n + 1):
        t = i / (n + 1.0)
        wa, wb = warp_pair(PA, PB, ca, cb, t, mode, alpha)
        m = blend(wa, wb, t)
        col, a = unpremult(m)
        out.append(np.dstack([col, a]).astype(np.float32))
    return out, sc, (mode, alpha)
