import numpy as np
from PIL import Image
U = '/mnt/user-data/uploads/'
SRC = {
 'START36': ('ChatGPT_Image_5_oct_2026__06_46_22.png', 6, 6, list(range(1, 37))),
 'MINI':    ('ChatGPT_Image_5_oct_2026__09_39_32.png', 4, 2, [4, 6, 16, 23, 25, 27, 33]),
 'END36':   ('ChatGPT_Image_5_oct_2026__09_58_46.png', 6, 6, list(range(1, 37))),
 'FIVE':    ('ChatGPT_Image_5_oct_2026__10_11_18.png', 3, 2, [6, 1, 2, 15, 25]),
 'MID36':   ('ChatGPT_Image_5_oct_2026__11_31_23.png', 6, 6, list(range(1, 37))),
 'MID7':    ('ChatGPT_Image_5_oct_2026__11_36_13.png', 3, 3, [1, 6, 10, 11, 12, 13, 15]),
 'JS':      ('ChatGPT_Image_5_oct_2026__10_37_14.png', 3, 3, [1, 2, 3, 4, 5, 6, 7]),
 'JE':      ('ChatGPT_Image_5_oct_2026__10_41_43.png', 3, 3, [1, 2, 3, 4, 5, 6, 7]),
}
_cache = {}
def _img(name):
    if name not in _cache:
        _cache[name] = Image.open(U + SRC[name][0]).convert('RGB')
    return _cache[name]

def _lines(prof, n_cells, size):
    """separator lines along one axis: strongest narrow bright peaks, one per boundary"""
    from scipy.signal import medfilt
    d = prof - medfilt(prof, 21)
    order = np.argsort(d)[::-1]
    chosen = []
    min_gap = size / (n_cells * 2.0)
    for i in order:
        if d[i] < 4: break
        if i < 4 or i > size - 5: continue
        if all(abs(i - j) > min_gap for j in chosen):
            chosen.append(int(i))
        if len(chosen) == n_cells - 1: break
    if len(chosen) != n_cells - 1:
        chosen = [int(round(i * size / n_cells)) for i in range(1, n_cells)]
    chosen.sort()
    return [0] + chosen + [size]

_grid = {}
def grid(src):
    if src in _grid: return _grid[src]
    f, cols, rows, order = SRC[src]
    im = np.asarray(_img(src)).astype(float)
    H, W, _ = im.shape
    g = im.min(axis=2)
    _grid[src] = (_lines(g.mean(axis=0), cols, W), _lines(g.mean(axis=1), rows, H))
    return _grid[src]

def cell(src, n, pad=4):
    """crop cell n of grid src using the detected separator lines"""
    f, cols, rows, order = SRC[src]
    im = _img(src)
    xs, ys = grid(src)
    idx = order.index(n)
    r, c = divmod(idx, cols)
    x0 = xs[c] + (pad if c > 0 else 0); x1 = xs[c + 1] - (pad if c + 1 < cols else 0)
    y0 = ys[r] + (pad if r > 0 else 0); y1 = ys[r + 1] - (pad if r + 1 < rows else 0)
    return im.crop((x0, y0, x1, y1))

def single(name):
    return Image.open(U + name).convert('RGB')

# which source holds each pose of exercise number n (1..36)
def start_of(n):
    if n == 6: return ('FIVE', 6)
    if n in (4, 16, 23, 25, 27, 33): return ('MINI', n)
    return ('START36', n)
def mid_of(n):
    if n in (1, 6, 10, 11, 12, 13, 15): return ('MID7', n)
    return ('MID36', n)
def end_of(n):
    if n == 6: return ('MINI', 6)
    if n in (1, 2, 15, 25): return ('FIVE', n)
    return ('END36', n)
