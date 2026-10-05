import numpy as np, cv2
from PIL import Image

def to_lab(rgb):
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2LAB).astype(np.float32)

def matte(pil, ring=0.05, number_box=(0.30, 0.20)):
    """returns RGBA PIL image with background removed and the cell number deleted"""
    rgb = np.asarray(pil).copy()
    h, w, _ = rgb.shape
    sm = cv2.GaussianBlur(rgb, (0, 0), 0.8)
    lab = to_lab(sm)
    # background colour: median of the outer ring, skipping the number box
    ys, xs = np.mgrid[0:h, 0:w]
    rw, rh = int(w * ring), int(h * ring)
    ringm = (xs < rw) | (xs >= w - rw) | (ys < rh) | (ys >= h - rh)
    ringm &= ~((xs < w * number_box[0]) & (ys < h * number_box[1]))
    bg = np.median(lab[ringm], axis=0)
    dist = np.sqrt(((lab - bg) ** 2).sum(axis=2))
    thr_lo, thr_hi = 5.0, 14.0
    prob = (dist > thr_lo).astype(np.uint8)
    core = (dist > thr_hi).astype(np.uint8)
    n, lbl = cv2.connectedComponents(prob, connectivity=8)
    keep = np.zeros(n, bool)
    keep[np.unique(lbl[core > 0])] = True
    keep[0] = False
    mask = keep[lbl].astype(np.uint8)
    # delete the cell number: dark components fully inside the top-left box
    nb_w, nb_h = int(w * number_box[0]), int(h * number_box[1])
    n2, lbl2, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    gray = cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY)
    for i in range(1, n2):
        x, y, cw_, ch_, area = stats[i]
        if x + cw_ <= nb_w + 2 and y + ch_ <= nb_h + 2 and gray[lbl2 == i].min() < 70:
            mask[lbl2 == i] = 0
    # remove tiny specks
    n3, lbl3, st3, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    big = st3[1:, cv2.CC_STAT_AREA].max() if n3 > 1 else 0
    for i in range(1, n3):
        if st3[i, cv2.CC_STAT_AREA] < max(25, big * 0.002):
            mask[lbl3 == i] = 0
    # fill holes
    inv = (1 - mask).astype(np.uint8)
    n4, lbl4 = cv2.connectedComponents(inv, connectivity=4)
    border_labels = set(np.unique(np.concatenate([lbl4[0], lbl4[-1], lbl4[:, 0], lbl4[:, -1]])))
    areas = np.bincount(lbl4.ravel(), minlength=n4)
    limit = max(60, int(0.0025 * h * w))
    holes = np.isin(lbl4, [i for i in range(1, n4) if i not in border_labels and areas[i] <= limit])
    mask[holes] = 1
    # alpha: solid inside, soft ramp on the boundary band
    k = np.ones((3, 3), np.uint8)
    inner = cv2.erode(mask, k, iterations=1)
    outer = cv2.dilate(mask, k, iterations=1)
    ramp = np.clip((dist - 3.0) / (12.0 - 3.0), 0, 1)
    alpha = np.where(inner > 0, 1.0, np.where(outer > 0, ramp, 0.0)).astype(np.float32)
    alpha = cv2.GaussianBlur(alpha, (0, 0), 0.6)
    rgba = np.dstack([rgb, (alpha * 255).astype(np.uint8)])
    return Image.fromarray(rgba, 'RGBA')

def on_stage(rgba, bg=(190, 192, 198)):
    base = Image.new('RGBA', rgba.size, bg + (255,))
    base.alpha_composite(rgba)
    return base.convert('RGB')
