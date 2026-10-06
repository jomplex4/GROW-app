import sys, os, json, time, shutil
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
from PIL import Image
import build_anim as ba
from build_anim import prepare, render_frames, composite, sift_match, alpha_bbox, save_webp, gray_for_flow, PRE
from mls_morph import keypoints, fix_swaps, mls_inbetweens, blend, premult, unpremult, warp_pair, control_points, quality, n_components, n_holes, to_pil
import mls_morph as mm
from kp import detect
from build2 import flow_pair, flow_inbetweens
from run_all import job, EXJ, main_ids, jaw_ids, OUT, FLIP_START

# ---------------------------------------------------------------- per exercise animation design
# loop = seconds for one side; profile = (rise, hold, fall) fractions of the loop (the rest is a pause at the start pose)
# mode: 'pp' ping-pong start->end->start, 'cy' forward cycle (gait); alt: 1 mirrored second side, 2 mirrored with a quick dip
T = {
 'march':      dict(loop=1.30, prof=(.45, .05, .45), alt=1),
 'armcirc':    dict(loop=3.20, prof=(.40, .10, .40)),
 'jj':         dict(loop=1.40, prof=(.45, .05, .45)),
 'legswing':   dict(loop=2.40, prof=(.50, .00, .50)),
 'highknee':   dict(loop=0.95, prof=(.45, .05, .45), alt=1),
 'buttkick':   dict(loop=0.95, prof=(.45, .05, .45), alt=1),
 'rope':       dict(loop=1.10, prof=(.50, .00, .50)),
 'squatjump':  dict(loop=2.40, prof=(.40, .15, .35)),
 'lungejump':  dict(loop=2.60, prof=(.45, .05, .45)),
 'walk':       dict(loop=1.70, prof=(.50, .00, .50)),
 'jog':        dict(loop=1.10, mode='cy'),
 'run':        dict(loop=0.95, mode='cy'),
 'sprint':     dict(loop=0.80, mode='cy'),
 'hang':       dict(loop=5.00, prof=(.40, .20, .40), br=1),
 'kneeraise':  dict(loop=3.20, prof=(.40, .20, .30)),
 'cobra':      dict(loop=5.50, prof=(.30, .40, .20)),
 'child':      dict(loop=6.50, prof=(.30, .45, .20)),
 'catcow':     dict(loop=5.00, prof=(.40, .10, .40)),
 'knees2chest':dict(loop=5.50, prof=(.30, .40, .20)),
 'elongate':   dict(loop=6.00, prof=(.30, .40, .20)),
 'plank':      dict(loop=6.00, prof=(.40, .20, .40), br=1),
 'mclimb':     dict(loop=1.50, prof=(.45, .05, .45)),
 'burpee':     dict(loop=3.80, prof=(.40, .15, .35)),
 'wallangel':  dict(loop=4.00, prof=(.40, .10, .40)),
 'wallstand':  dict(loop=6.00, prof=(.30, .40, .20), br=1),
 'reach':      dict(loop=4.00, prof=(.35, .25, .30)),
 'deadbug':    dict(loop=3.60, prof=(.35, .20, .35), alt=2),
 'birddog':    dict(loop=4.00, prof=(.35, .30, .25), alt=2),
 'superman':   dict(loop=4.50, prof=(.30, .35, .25)),
 'bridge':     dict(loop=3.60, prof=(.35, .25, .30)),
 'calf':       dict(loop=6.00, prof=(.30, .45, .20)),
 'quadstretch':dict(loop=6.00, prof=(.30, .45, .20)),
 'hipflex':    dict(loop=6.00, prof=(.30, .45, .20)),
 'hamfold':    dict(loop=6.50, prof=(.35, .40, .20)),
 'sidebend':   dict(loop=5.00, prof=(.35, .35, .25), alt=1),
 'chestopen':  dict(loop=5.00, prof=(.30, .40, .20)),
 'chintuck':   dict(loop=4.00, prof=(.30, .35, .25)),
 'jawopen':    dict(loop=4.00, prof=(.35, .25, .30)),
 'jawresist':  dict(loop=4.50, prof=(.30, .40, .20)),
 'necktilt':   dict(loop=5.00, prof=(.30, .40, .20), alt=1),
 'neckflex':   dict(loop=5.00, prof=(.30, .40, .20)),
 'tonguepos':  dict(loop=6.00, prof=(.30, .45, .20), br=1),
 'jawmassage': dict(loop=2.40, prof=(.50, .00, .50)),
}
NO_MID = {'legswing', 'bridge', 'superman', 'birddog', 'hamfold', 'walk'}   # mid pose too close to the end (or a stride) -> 2 keys
GAIT = {'jog', 'run', 'sprint'}
HANG = {'hang', 'kneeraise'}
HANDS = {'burpee'}
FORCE_FLOW = {('quadstretch', 0), ('run', 1)}
FORCE_MLS = {('child', 0), ('hamfold', 0)}
MATX = {'cobra', 'child', 'catcow', 'knees2chest', 'elongate', 'plank', 'mclimb', 'deadbug', 'birddog', 'superman', 'bridge', 'hipflex', 'hamfold'}

def mat_anchors(A, B):
    al = np.maximum(A[..., 3], B[..., 3])
    ys, xs = np.where(al > 128)
    if len(ys) == 0: return None
    y = ys.max() - 0.012 * A.shape[0]
    x0, x1 = xs.min(), xs.max()
    return np.array([[x0 + (x1 - x0) * k / 10.0, y] for k in range(11)], np.float64)

BONES_S = [((1, 2), None), ((7, 8), None), ((1, 3), None), ((2, 4), None), ((3, 5), None), ((4, 6), None),
           ((7, 9), None), ((8, 10), None), ((9, 11), None), ((10, 12), None)]

def torso_pts(k):
    return (k[1, :2] + k[2, :2]) / 2, (k[7, :2] + k[8, :2]) / 2

def scale_from_kp(k0, kj, minv=0.6):
    """size ratio from the bones that did NOT move between the two poses (moving limbs foreshorten and lie)"""
    ok0 = k0[:, 2] > minv; okj = kj[:, 2] > minv
    segs = []
    if ok0[[1, 2, 7, 8]].all() and okj[[1, 2, 7, 8]].all():
        s0, h0 = torso_pts(k0); sj, hj = torso_pts(kj)
        segs.append((s0, h0, sj, hj, 2))
    for (a, b), _ in BONES_S:
        if ok0[a] and ok0[b] and okj[a] and okj[b]:
            segs.append((k0[a, :2], k0[b, :2], kj[a, :2], kj[b, :2], 1))
    still, allr = [], []
    for p0, q0, pj, qj, w in segs:
        v0 = q0 - p0; vj = qj - pj
        l0, lj = np.linalg.norm(v0), np.linalg.norm(vj)
        if l0 < 8 or lj < 8: continue
        r = l0 / lj
        if not (0.6 < r < 1.8): continue
        cosang = float(np.dot(v0, vj) / (l0 * lj))
        allr += [r] * w
        if cosang > np.cos(np.radians(15)): still += [r] * w
    if len(still) >= 3: return float(np.median(still))
    if len(allr) >= 4: return float(np.median(allr))
    return None

def register_v2(rgbas, id_, kind):
    pres, rs = [], []
    for im in rgbas:
        r = PRE / max(im.size); rs.append(r)
        pres.append(im.resize((int(im.width * r), int(im.height * r)), Image.LANCZOS))
    kps = []
    for p in pres:
        try: kps.append(detect(p))
        except Exception: kps.append(None)
    mats = [np.eye(3)]; info = [('ref', '')]
    for j in range(1, len(pres)):
        i = j - 1                              # chain: each pose is aligned to the previous one (most similar)
        ai = np.asarray(pres[i])[..., 3]; Xi = alpha_bbox(ai)
        aj = np.asarray(pres[j])[..., 3]; Xj = alpha_bbox(aj)
        sc, how = None, ''
        if kind == 'A' and kps[i] is not None and kps[j] is not None:
            sc = scale_from_kp(kps[i], kps[j]); how = 'kp'
        if sc is None:
            M, inl = sift_match(composite(pres[i]), composite(pres[j]))
            if M is not None and inl >= 8: sc, how = float(M[0, 0]), 'sift%d' % inl
        if sc is None: sc, how = 1.0, 'none'
        sc = float(np.clip(sc, 0.6, 1.7))
        if kind == 'B':
            tx = (Xi[0] + Xi[2]) / 2 - sc * (Xj[0] + Xj[2]) / 2; ty = Xi[3] - sc * Xj[3]
        elif id_ in HANG:
            tx = (Xi[0] + Xi[2]) / 2 - sc * (Xj[0] + Xj[2]) / 2; ty = Xi[1] - sc * Xj[1]
        else:
            hxi = hxj = None
            ia, ib = (5, 6) if id_ in HANDS else (7, 8)
            if kps[i] is not None and kps[j] is not None and min(kps[i][ia, 2], kps[i][ib, 2], kps[j][ia, 2], kps[j][ib, 2]) > .5:
                hxi = (kps[i][ia, 0] + kps[i][ib, 0]) / 2; hxj = (kps[j][ia, 0] + kps[j][ib, 0]) / 2
            if hxi is None: hxi = (Xi[0] + Xi[2]) / 2; hxj = (Xj[0] + Xj[2]) / 2
            tx = hxi - sc * hxj; ty = Xi[3] - sc * Xj[3]
        rel = np.array([[sc, 0, tx], [0, sc, ty], [0, 0, 1]], np.float64)
        mats.append(mats[i] @ rel); info.append((how, round(sc, 3)))
    return mats, rs, pres, info

def swap_lr(k):
    k = k.copy()
    for a, b in [(3, 4), (5, 6), (9, 10), (11, 12), (13, 14), (15, 16)]:
        k[[a, b]] = k[[b, a]]
    return k

def inb(A, B, ka, kb, n, dis, id_, seg):
    """in-between frames A->B: skeleton-driven when reliable, optical flow otherwise"""
    wa, wb = flow_pair(A, B, 0.5, dis); sc_flow = quality(wa, wb, A, B)
    if ka is not None and kb is not None and (id_, seg) not in FORCE_FLOW:
        ok = (ka[:, 2] > 0.45) & (kb[:, 2] > 0.45)
        disp = float(np.linalg.norm(ka[ok, :2] - kb[ok, :2], axis=1).mean()) if ok.sum() else 0.0
        forced = (id_, seg) in FORCE_MLS
        if forced or (ok.sum() >= 12 and ka[:, 2].mean() > 0.55 and kb[:, 2].mean() > 0.55 and disp >= 12):
            ins, sc, c = mls_inbetweens(A, B, ka, kb, n, anchors=mat_anchors(A, B) if id_ in MATX else None)
            if ins is not None and (forced or sc >= sc_flow - 0.12):
                aA = (A[..., 3] > 128).sum(); aB = (B[..., 3] > 128).sum()
                cmax = max(n_components(A[..., 3] > 128), n_components(B[..., 3] > 128))
                hmax = max(n_holes(A[..., 3] > 128), n_holes(B[..., 3] > 128))
                fl = None
                for j, fr in enumerate(ins):
                    t = (j + 1) / (n + 1.0); m = fr[..., 3] > 128
                    exp = max(1.0, (1 - t) * aA + t * aB)
                    if abs(m.sum() - exp) / exp > 0.16 or n_components(m) > cmax or n_holes(m) > hmax + 1:
                        if fl is None: fl = flow_inbetweens(A, B, n, dis)
                        ins[j] = fl[j]
                return ins, 'mls'
    return flow_inbetweens(A, B, n, dis), 'flow'

HEADROT = {'neckflex', 'necktilt'}

def head_split(F):
    """(head layer, body layer) of a close-up: everything above the polo collar line belongs to the head"""
    from scipy.ndimage import median_filter
    r, g, b, a = F[..., 0], F[..., 1], F[..., 2], F[..., 3]
    red = ((r > 120) & (g < 100) & (b < 100) & (a > 60)).astype(np.uint8)
    n, lbl, st, _ = cv2.connectedComponentsWithStats(red, connectivity=8)
    H, W = a.shape
    if n <= 1:
        polo = np.zeros_like(red, bool)
    else:
        big = 1 + int(np.argmax(st[1:, cv2.CC_STAT_AREA]))      # the shirt, not the lips
        polo = cv2.dilate((lbl == big).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    ys, xs = np.where(polo)
    top_global = int(ys.min()) if len(ys) else int(H * 0.7)
    top = np.full(W, H, np.int32)
    for x in range(W):
        col = np.where(polo[:, x])[0]
        if len(col): top[x] = col.min()
    # columns without shirt (in front of the face / behind the head) take the nearest shirt column's line
    has = top < H
    if has.any():
        idx = np.where(has)[0]
        for x in range(W):
            if not has[x]: top[x] = top[idx[np.argmin(np.abs(idx - x))]]
    top = median_filter(top, size=15).astype(np.int32)
    yy = np.arange(H)[:, None]
    above = yy < top[None, :]
    head = F.copy(); body = F.copy()
    head[..., 3] = np.where(above, a, 0); body[..., 3] = np.where(above, 0, a)
    near_top = ys < top_global + 0.04 * H
    cx = float(np.median(xs[near_top])) if near_top.any() else W / 2
    return head, body, (cx, float(top_global))

def rot(F, ang, piv, d=(0.0, 0.0)):
    M = cv2.getRotationMatrix2D(piv, ang, 1.0)
    M[0, 2] += d[0]; M[1, 2] += d[1]
    P = premult(F)
    return cv2.warpAffine(P, M, (F.shape[1], F.shape[0]), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT, borderValue=0)

def centroid(m):
    ys, xs = np.where(m)
    return np.array([xs.mean(), ys.mean()]) if len(xs) else np.zeros(2)

ANG_RANGE = {'neckflex': range(-70, -9, 2), 'necktilt': range(-45, 46, 2)}

def head_rotation_inbetweens(A, B, n, id_):
    hA, bA, pA = head_split(A); hB, bB, pB = head_split(B)
    piv = ((pA[0] + pB[0]) / 2, (pA[1] + pB[1]) / 2)
    mB = hB[..., 3] > 128; cB = centroid(mB)
    best = (0, (0.0, 0.0), -1.0)
    for ang in ANG_RANGE.get(id_, range(-60, 61, 2)):
        w = rot(hA, ang, piv)[..., 3] > 128
        d = cB - centroid(w)
        w2 = rot(hA, ang, piv, d)[..., 3] > 128
        iou = (w2 & mB).sum() / max(1, (w2 | mB).sum())
        if iou > best[2]: best = (ang, (float(d[0]), float(d[1])), iou)
    th, d, iou = best
    H = A.shape[0]
    out = []
    for i in range(1, n + 1):
        t = i / (n + 1.0)
        wa = rot(hA, th * t, piv, (d[0] * t, d[1] * t))
        # inverse of B's transform scaled by (1 - t)
        wb = rot(hB, -th * (1 - t), piv, (-d[0] * (1 - t), -d[1] * (1 - t)))
        head = blend(wa, wb, t)
        nearH, nearB, near_top = (hA, bA, pA[1]) if t < 0.5 else (hB, bB, pB[1])
        # neck stub of the nearest pose stays in place so no gap opens above the collar
        stub = nearH.copy()
        W = A.shape[1]
        yy = np.arange(H)[:, None]; xx = np.arange(W)[None, :]
        lum = nearH[..., :3].mean(axis=2)
        keep = (yy > near_top - 0.09 * H) & (np.abs(xx - piv[0]) < 0.16 * W) & (lum > 70)
        stub[..., 3] = np.where(keep, stub[..., 3], 0)
        P = premult(nearB)
        S = premult(stub); P = S + P * (1 - S[..., 3:4] / 255.0)
        al = head[..., 3:4] / 255.0
        m = head + P * (1 - al)
        col, a = unpremult(m)
        out.append(np.dstack([col, a]).astype(np.float32))
    return out, th, iou

def build_one(kind, n, id_):
    mm.SOFT[0] = (id_ == 'rope')
    keys = job('A' if kind == 'A' else 'B', n, id_)
    if id_ in NO_MID and len(keys) == 3: keys = [keys[0], keys[2]]
    flips = [kind == 'A' and n in FLIP_START] + [False] * (len(keys) - 1)
    rgbas = [prepare(p, flip=flips[i]) for i, p in enumerate(keys)]
    mats, rs, pres, info = register_v2(rgbas, id_, kind)
    F, size = render_frames(rgbas, mats, rs, pres)
    try:
        kps = keypoints(F); kps = fix_swaps(kps) if kps else None
    except Exception:
        kps = None
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    n_in = 3 if len(F) >= 3 else 7
    seq, meth = [], []
    if id_ in GAIT and kps is not None and len(F) == 3:
        # cycle start -> passing -> end -> start: the legs keep alternating instead of rewinding the same leg
        chain = [(F[0], kps[0]), (F[1], kps[1]), (F[2], kps[2]), (F[0], kps[0])]
        for i in range(3):
            A, ka = chain[i]; B, kb = chain[i + 1]
            seq.append(A); ins, how = inb(A, B, ka, kb, 3, dis, id_, i); seq += ins; meth.append(how)
        mode = 'cy'
    elif id_ in HEADROT:
        ins, th, iou = head_rotation_inbetweens(F[0], F[1], n_in, id_)
        seq = [F[0]] + ins + [F[1]]; meth.append('headrot %d deg iou %.2f' % (th, iou)); mode = 'pp'
    else:
        for i in range(len(F) - 1):
            seq.append(F[i])
            ins, how = inb(F[i], F[i + 1], kps[i] if kps else None, kps[i + 1] if kps else None, n_in, dis, id_, i)
            seq += ins; meth.append(how)
        seq.append(F[-1])
        mode = 'pp'
    return seq, size, info, meth, mode

def run(only=None):
    os.makedirs(OUT, exist_ok=True)
    meta = json.load(open(OUT + '/meta.json')) if os.path.exists(OUT + '/meta.json') else {}
    todo = [('A', i + 1, id_) for i, id_ in enumerate(main_ids)] + [('B', i + 1, id_) for i, id_ in enumerate(jaw_ids)]
    for kind, n, id_ in todo:
        if only and id_ not in only: continue
        t0 = time.time()
        seq, size, info, meth, mode = build_one(kind, n, id_)
        d = f'{OUT}/{id_}'
        shutil.rmtree(d, ignore_errors=True); os.makedirs(d)
        total = 0
        for k, f in enumerate(seq): total += save_webp(f, f'{d}/{k:02d}.webp', 76)
        t = T[id_]; prof = t.get('prof', (.5, 0, .5))
        meta[id_] = dict(n=len(seq), w=size[0], h=size[1], alt=t.get('alt', 0), step=0, cyc=0, bust=int(kind == 'B'),
                         ground=int(not (kind == 'B' or id_ in HANG)), loop=t['loop'], mode=t.get('mode', mode),
                         ri=prof[0], ho=prof[1], fa=prof[2], br=t.get('br', 0))
        json.dump(meta, open(OUT + '/meta.json', 'w'), separators=(',', ':'))
        print(id_, info, meth, mode, '%dKB' % (total // 1024), round(time.time() - t0, 1), 's', flush=True)

if __name__ == '__main__':
    only = sys.argv[1].split(',') if len(sys.argv) > 1 and sys.argv[1] != 'all' else None
    run(only)
