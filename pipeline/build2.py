import sys, os, json, time, shutil
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
from PIL import Image
import build_anim as ba
from build_anim import *
from mls_morph import *
from run_all import job, CFG, FLIP_START, ALT, EXJ, main_ids, jaw_ids, OUT
CYC = {1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 22, 23}      # fast cyclic moves: no pause at the ends of the loop

def flow_pair(A, B, t, dis):
    gA, gB = gray_for_flow(A), gray_for_flow(B)
    fab = dis.calc(gA, gB, None); fba = dis.calc(gB, gA, None)
    H, W = gA.shape
    gx, gy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(H, dtype=np.float32))
    pa, pb = premult(A), premult(B)
    wa = cv2.remap(pa, gx + t * fba[..., 0], gy + t * fba[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    wb = cv2.remap(pb, gx + (1 - t) * fab[..., 0], gy + (1 - t) * fab[..., 1], cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    return wa, wb

def flow_inbetweens(A, B, n, dis):
    out = []
    for i in range(1, n + 1):
        t = i / (n + 1.0)
        wa, wb = flow_pair(A, B, t, dis)
        m = blend(wa, wb, t)
        col, a = unpremult(m)
        a2 = np.clip((a / 255.0 - 0.56) * 2.4 + 0.5, 0, 1) * 255
        out.append(np.dstack([col, a2]).astype(np.float32))
    return out

FORCE_FLOW = {('quadstretch', 0)}
CUR_ID = ['']

def frames_auto(key_pils, cfg, flips, n_in):
    rgbas = [prepare(p, flip=bool(flips and flips[i])) for i, p in enumerate(key_pils)]
    mats, rs, pres, info = register(rgbas, cfg)
    F, size = render_frames(rgbas, mats, rs, pres)
    try:
        kps = keypoints(F); kps = fix_swaps(kps) if kps else None
    except Exception:
        kps = None
    dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
    seq, methods = [], []
    for i in range(len(F) - 1):
        seq.append(F[i])
        wa, wb = flow_pair(F[i], F[i + 1], 0.5, dis); sc_flow = quality(wa, wb, F[i], F[i + 1])
        res = None
        if kps is not None:
            pa, pb = kps[i], kps[i + 1]
            ok = (pa[:, 2] > 0.45) & (pb[:, 2] > 0.45)
            disp = float(np.linalg.norm(pa[ok, :2] - pb[ok, :2], axis=1).mean()) if ok.sum() else 0.0
            good = ok.sum() >= 12 and pa[:, 2].mean() > 0.55 and pb[:, 2].mean() > 0.55
            if good and disp >= 12 and (CUR_ID[0], i) not in FORCE_FLOW:
                ins, sc, c = mls_inbetweens(F[i], F[i + 1], pa, pb, n_in)
                if ins is not None and sc >= sc_flow - 0.12:
                    aA = (F[i][..., 3] > 128).sum(); aB = (F[i + 1][..., 3] > 128).sum()
                    cmax = max(n_components(F[i][..., 3] > 128), n_components(F[i + 1][..., 3] > 128))
                    hmax = max(n_holes(F[i][..., 3] > 128), n_holes(F[i + 1][..., 3] > 128))
                    fl = None; fixed = 0
                    for j, fr in enumerate(ins):
                        t = (j + 1) / (n_in + 1.0); m = fr[..., 3] > 128
                        exp = max(1.0, (1 - t) * aA + t * aB)
                        if abs(m.sum() - exp) / exp > 0.16 or n_components(m) > cmax or n_holes(m) > hmax + 1:
                            if fl is None: fl = flow_inbetweens(F[i], F[i + 1], n_in, dis)
                            ins[j] = fl[j]; fixed += 1
                    res = (ins, 'mls-%s-%.1f d%d fix%d' % (c[0], c[1], disp, fixed), sc)
        if res is None:
            res = (flow_inbetweens(F[i], F[i + 1], n_in, dis), 'flow', sc_flow)
        seq += res[0]; methods.append((res[1], round(float(res[2]), 2)))
    seq.append(F[-1])
    return seq, F, size, info, methods

def run(only=None):
    os.makedirs(OUT, exist_ok=True)
    meta = json.load(open(OUT + '/meta.json')) if os.path.exists(OUT + '/meta.json') else {}
    todo = [('A', i + 1, id_) for i, id_ in enumerate(main_ids)] + [('B', i + 1, id_) for i, id_ in enumerate(jaw_ids)]
    for kind, n, id_ in todo:
        if only and id_ not in only: continue
        t0 = time.time()
        CUR_ID[0] = id_
        import mls_morph as mm
        mm.SOFT[0] = (kind == 'A' and n == 7)
        keys = job('A' if kind == 'A' else 'B', n, id_)
        n_in = 3 if len(keys) == 3 else 7
        flips = [kind == 'A' and n in FLIP_START] + [False] * (len(keys) - 1)
        seq, F, size, info, methods = frames_auto(keys, CFG.get(n) if kind == 'A' else None, flips, n_in)
        d = f'{OUT}/{id_}'
        shutil.rmtree(d, ignore_errors=True); os.makedirs(d)
        total = 0
        for k, f in enumerate(seq): total += save_webp(f, f'{d}/{k:02d}.webp', 80)
        meta[id_] = dict(n=len(seq), w=size[0], h=size[1], alt=int(kind == 'A' and n in ALT), step=0,
                         cyc=int(kind == 'A' and n in CYC), bust=int(kind == 'B'),
                         ground=int(not (kind == 'B' or id_ in ('hang', 'kneeraise'))))
        json.dump(meta, open(OUT + '/meta.json', 'w'), separators=(',', ':'))
        print(id_, methods, '%d KB' % (total // 1024), round(time.time() - t0, 1), 's', flush=True)

if __name__ == '__main__':
    only = sys.argv[1].split(',') if len(sys.argv) > 1 and sys.argv[1] != 'all' else None
    run(only)
