"""GROW v2.0 plan generator: writes assets/plan.txt (1292 lines, one per day).
Step format: exerciseIndex:seconds:rest:flag:tag   (rest -1 = user's rest setting, 0 = continuous, N = base seconds scaled by the setting)
Tags: W warm-up, I impact, R run, S strength, C core/posture, D decompression, F flexibility"""
import json, datetime, os, sys, collections

A = '/home/claude/grow/app/src/main/assets'
EX = json.load(open(A + '/exercises.json'))
IDX = {e['id']: i for i, e in enumerate(EX)}
SIDED = {e['id']: e['sd'] for e in EX}
START = datetime.date(2026, 10, 11); END = datetime.date(2030, 4, 18)
TOTAL = (END - START).days + 1
PH_START = [1, 91, 366, 731, 1101, TOTAL + 1]

def phase(d): return 0 if d <= 90 else 1 if d <= 365 else 2 if d <= 730 else 3 if d <= 1100 else 4
def prog_in_phase(d):
    ph = phase(d); a, b = PH_START[ph], PH_START[ph + 1]
    return (d - a) / max(1, (b - a - 1))
def r5(x): return int(round(x / 5.0) * 5)
def lerp(a, b, t): return a + (b - a) * t

# exercises unlocked per phase (index of the phase where they appear, 1 = from the first day)
LV = dict(rope=1, jj=1, squatjump=1, highknee=1, lungejump=2, burpee=3, mclimb=1, kneeraise=2, deadbug=1, birddog=1,
          superman=1, bridge=1, plank=1)
def ok(id_, ph): return LV.get(id_, 1) <= ph + 1

# base seconds (start, end after ~18 months) for non impact exercises
DUR = dict(march=(30, 50), armcirc=(30, 45), jj=(30, 50), legswing=(25, 35), highknee=(25, 45), buttkick=(30, 45),
           rope=(30, 60), squatjump=(25, 40), lungejump=(25, 40), mclimb=(30, 50), burpee=(20, 40),
           hang=(20, 60), kneeraise=(20, 45), cobra=(30, 60), child=(30, 75), catcow=(30, 60), knees2chest=(30, 60),
           elongate=(30, 60), plank=(30, 90), wallangel=(30, 60), wallstand=(30, 90), reach=(30, 45), deadbug=(30, 60),
           birddog=(30, 60), superman=(30, 50), bridge=(30, 60), calf=(30, 45), quadstretch=(30, 45), hipflex=(30, 50),
           hamfold=(30, 60), sidebend=(30, 50), chestopen=(30, 45))
def secs(id_, d, deload):
    lo, hi = DUR[id_]
    p = min(1.0, (d - 1) / 540.0) ** 0.85
    v = lo + (hi - lo) * p
    if phase(d) == 4: v *= 0.92
    if deload: v *= 0.8
    return max(15, r5(v))

WARM = ['march', 'armcirc', 'legswing', 'buttkick', 'jj', 'highknee']
IMPACT = ['rope', 'squatjump', 'jj', 'lungejump', 'burpee', 'highknee', 'mclimb']
STRENGTH = ['plank', 'deadbug', 'birddog', 'superman', 'bridge', 'kneeraise', 'mclimb', 'burpee']
POSTURE = ['wallangel', 'wallstand', 'reach', 'superman', 'bridge']
DECOMP = ['hang', 'cobra', 'child', 'catcow', 'knees2chest', 'elongate']
FLEX = ['calf', 'quadstretch', 'hipflex', 'hamfold', 'sidebend', 'chestopen']

# effective minutes of jumping per impact day (start, end of each phase)
DOSE = [(3.0, 4.5), (4.5, 6.0), (6.0, 7.5), (7.0, 8.5), (6.0, 7.0)]
INT_LEN = [30, 35, 40, 40, 35]
IMP_REST = [20, 20, 15, 15, 15]
STR_REST = [20, 20, 15, 15, 15]
W_COUNT = [4, 4, 4, 5, 4]; C_COUNT = [3, 3, 4, 4, 3]; S_COUNT = [4, 4, 5, 5, 4]; S_ROUNDS = [2, 2, 3, 3, 2]; D_COUNT = [3, 3, 3, 4, 3]; F_COUNT = [3, 3, 3, 3, 3]

def pick(pool, n, seed, used):
    out = []; k = seed
    cand = [x for x in pool if x not in used]
    if not cand: cand = list(pool)
    for _ in range(len(cand) * 2):
        x = cand[k % len(cand)]; k += 1
        if x not in out: out.append(x)
        if len(out) == n: break
    used.update(out)
    return out

def steps_for(ids, d, deload, rest, tag, override=None):
    res = []
    for i in ids:
        w = override if override else secs(i, d, deload)
        if SIDED[i]:
            res.append((i, w, 5, 0, tag)); res.append((i, w, rest, 1, tag))
        else:
            res.append((i, w, rest, 0, tag))
    return res

def impact_block(d, ph, deload, seed, used):
    lo, hi = DOSE[ph]; D = lerp(lo, hi, prog_in_phase(d)) * (0.7 if deload else 1.0)
    w0 = INT_LEN[ph]
    pool = [x for x in IMPACT if ok(x, ph)]
    k = min(len(pool), 5 if ph >= 2 else 4)
    order = ['jj', 'highknee', 'rope', 'squatjump', 'lungejump', 'burpee', 'mclimb']
    chosen = pick(pool, k, seed, used)
    chosen.sort(key=lambda x: order.index(x))
    k = len(chosen)
    rounds = max(1, -(-int(D * 60) // (k * w0)))          # ceil
    rounds = min(rounds, 3)
    w = max(20, min(45, r5(D * 60 / (k * rounds))))
    res = []
    for r in range(rounds):
        for i in chosen: res.append((i, w, IMP_REST[ph], 0, 'I'))
    return res

def strength_block(d, ph, deload, seed, used, friday=False):
    pool = [x for x in STRENGTH if ok(x, ph)]
    n = 3 if friday else S_COUNT[ph]
    rounds = (3 if ph in (2, 3) and not friday else 2) if not deload else 2
    if friday and ph == 3: rounds = 3
    ids = pick(pool, n, seed, used)
    order = {x: i for i, x in enumerate(['plank', 'bridge', 'deadbug', 'birddog', 'superman', 'kneeraise', 'mclimb', 'burpee'])}
    ids.sort(key=lambda x: order[x])
    res = []
    for r in range(rounds):
        for i in ids:
            w = max(25, r5(secs(i, d, deload) * 0.9))
            if SIDED[i]:
                res.append((i, w, 5, 0, 'S')); res.append((i, w, STR_REST[ph], 1, 'S'))
            else:
                res.append((i, w, STR_REST[ph], 0, 'S'))
    return res

def run_block(d, ph, deload, steady):
    t = prog_in_phase(d); R = []
    def add(i, s, rest=0): R.append((i, int(s), rest, 0, 'R'))
    if not steady:
        if ph == 0:
            add('walk', 120); reps = 4
            for _ in range(reps): add('jog', r5(40 + 20 * t)); add('walk', r5(80 - 20 * t))
        elif ph == 1:
            add('jog', 180)
            for _ in range(5): add('run', r5(60 + 30 * t)); add('jog', 60)
        elif ph == 2:
            add('jog', 180)
            for _ in range(4): add('run', r5(90 + 30 * t)); add('jog', 60)
            for _ in range(4): add('sprint', 15); add('walk', 45)
        elif ph == 3:
            add('jog', 150)
            for _ in range(4): add('run', r5(105 + 15 * t)); add('jog', 60)
            for _ in range(5): add('sprint', 20); add('walk', 40)
        else:
            add('jog', 180)
            for _ in range(3): add('run', 90); add('jog', 60)
            for _ in range(4): add('sprint', 15); add('walk', 45)
    else:
        if ph == 0: add('walk', 120); add('jog', r5(180 + 60 * t)); add('walk', 60); add('jog', 180)
        elif ph == 1: add('jog', r5(480 + 120 * t))
        elif ph == 2: add('jog', 180); add('run', r5(420 + 120 * t))
        elif ph == 3: add('jog', 180); add('run', r5(540 + 120 * t))
        else: add('jog', 180); add('run', 420)
    if deload and len(R) > 6: R = R[:len(R) - 4]
    out = [(IDX[i], s, rs, fl, tg) for (i, s, rs, fl, tg) in R]
    last = out[-1]; out[-1] = (last[0], last[1], -1, last[3], last[4])
    return out

ORDER = {0: 'WICD', 1: 'WRFD', 2: 'WSCD', 3: 'WIDF', 4: 'WRSF', 6: 'XDFC'}
def session(d, date):
    wd = date.weekday()
    if wd == 5: return None
    ph = phase(d); week = (d - 1) // 7; deload = (week % 4 == 3 and week > 0)
    seed = week * 3 + wd; used = set(); steps = []
    for b in ORDER[wd]:
        blk = []
        if b == 'W':
            wp = [x for x in WARM if ok(x, ph) and (wd not in (0, 3) or x not in ('jj', 'highknee'))]
            ids = pick(wp, W_COUNT[ph], seed, used)
            blk = steps_for(ids, d, deload, -1, 'W')
        elif b == 'I': blk = impact_block(d, ph, deload, seed + 1, used)
        elif b == 'S': blk = strength_block(d, ph, deload, seed + 2, used, friday=(wd == 4))
        elif b == 'C':
            n = 3 if wd == 2 else C_COUNT[ph]
            pool = POSTURE if wd in (2, 6) else ['wallangel', 'wallstand', 'reach', 'deadbug', 'birddog', 'superman', 'bridge', 'plank']
            ids = pick([x for x in pool if ok(x, ph)], n, seed + 3, used)
            blk = steps_for(ids, d, deload, -1, 'C')
        elif b == 'D':
            n = D_COUNT[ph] + (1 if wd in (3, 6) else 0) - (1 if wd == 1 else 0)
            ids = pick(DECOMP, n, seed + 4, used)
            if wd == 3 and 'hang' not in ids: ids[0] = 'hang'
            blk = steps_for(ids, d, deload, -1, 'D')
        elif b == 'F':
            n = F_COUNT[ph] - (1 if wd == 3 else 0)
            ids = pick(FLEX, n, seed + 5, used)
            blk = steps_for(ids, d, deload, -1, 'F')
        elif b == 'R': blk = run_block(d, ph, deload, steady=(wd == 4))
        elif b == 'X': blk = [(IDX['walk'], 240 if ph < 2 else 300, -1, 0, 'R')]
        steps += [(IDX[i] if isinstance(i, str) else i, s, rs, fl, tg) for (i, s, rs, fl, tg) in blk]
    return steps

def total_seconds(steps, rest_setting=12):
    t = 0
    for k, (i, sec, rs, fl, tg) in enumerate(steps):
        t += sec
        if k < len(steps) - 1:
            t += rest_setting if rs == -1 else (rs if rs <= 5 else round(rs * rest_setting / 12.0))
    return t

if __name__ == '__main__':
    lines = []; stats = collections.defaultdict(list); imp = collections.defaultdict(list)
    for d in range(1, TOTAL + 1):
        date = START + datetime.timedelta(days=d - 1)
        s = session(d, date)
        if s is None: lines.append('R'); continue
        lines.append(','.join(f'{i}:{sec}:{rs}:{fl}:{tg}' for (i, sec, rs, fl, tg) in s))
        stats[(phase(d), date.weekday())].append(total_seconds(s) / 60.0)
        imp[(phase(d), date.weekday())].append(sum(x[1] for x in s if x[4] == 'I') / 60.0)
    open(A + '/plan.txt', 'w').write('\n'.join(lines))
    print('plan lines', len(lines), 'bytes', os.path.getsize(A + '/plan.txt'))
    names = ['Base', 'Prog', 'Media', 'Alta', 'Consol']
    for ph in range(5):
        row = []
        for wd in (0, 1, 2, 3, 4, 6):
            v = stats[(ph, wd)]
            row.append('%s %.0f-%.0f' % ('LMXJVD'[(0, 1, 2, 3, 4, 6).index(wd)], min(v), max(v)))
        im = imp[(ph, 0)] + imp[(ph, 3)]
        print(names[ph], '| session min:', ' '.join(row), '| jump min/impact day: %.1f-%.1f' % (min(im), max(im)))
