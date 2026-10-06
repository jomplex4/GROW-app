import json, os, datetime, sys
A = '/home/claude/grow/app/src/main/assets'
ex = json.load(open(A + '/exercises.json')); meta = json.load(open(A + '/anim/meta.json'))
plan = open(A + '/plan.txt').read().split('\n'); jaw = open(A + '/jaw.txt').read().split('\n')
fails = []
def check(c, m):
    if not c: fails.append(m)
START = datetime.date(2026, 10, 5); END = datetime.date(2030, 4, 18); TOTAL = (END - START).days + 1
check(len(ex) == 43, f'exercises {len(ex)}')
ids = [e['id'] for e in ex]
check(len(set(ids)) == 43, 'duplicate ids')
# sprites
total_bytes = 0
for e in ex:
    i = e['id']; m = meta.get(i)
    check(m is not None, f'no sprite meta for {i}')
    if not m: continue
    d = f'{A}/anim/{i}'
    for k in range(m['n']):
        f = f'{d}/{k:02d}.webp'; check(os.path.exists(f), f'missing {f}'); total_bytes += os.path.getsize(f) if os.path.exists(f) else 0
    extra = [f for f in os.listdir(d) if f not in [f'{k:02d}.webp' for k in range(m['n'])]]
    check(not extra, f'unexpected files in {i}: {extra}')
    check(m['w'] > 50 and m['h'] > 50 and m['n'] >= 2, f'bad meta {i}')
check(set(meta) == set(ids), 'meta ids differ from exercises')
# plan
check(len(plan) == TOTAL, f'plan lines {len(plan)} vs {TOTAL}')
check(len(jaw) == TOTAL, f'jaw lines {len(jaw)} vs {TOTAL}')
sided = {i: e['sd'] for i, e in enumerate(ex)}
n_train = 0; mins = []; jmins = []
for d in range(1, TOTAL + 1):
    dt = START + datetime.timedelta(days=d - 1); sat = dt.weekday() == 5
    for name, lines in (('plan', plan), ('jaw', jaw)):
        line = lines[d - 1]
        if sat:
            check(line == 'R', f'{name} day {d} (Saturday) is not rest'); continue
        check(line != 'R' and line != '', f'{name} day {d} empty on a training day')
        steps = [s.split(':') for s in line.split(',')]
        check(len(steps) >= 5, f'{name} day {d} too few steps')
        sec = 0
        for k, s in enumerate(steps):
            check(len(s) == 5, f'{name} day {d} bad step {s}')
            xi, ss, rs, fl, tg = int(s[0]), int(s[1]), int(s[2]), int(s[3]), s[4]
            check(0 <= xi < 43, f'{name} day {d} bad exercise index {xi}')
            check(15 <= ss <= 305, f'{name} day {d} bad seconds {ss}')
            check(rs == -1 or 0 <= rs <= 60, f'{name} day {d} bad rest {rs}')
            check(tg in 'WIRDCFJ', f'{name} day {d} bad tag {tg}')
            if fl == 1:
                check(sided[xi] == 1, f'{name} day {d} flag on non sided {xi}')
                check(k > 0 and int(steps[k - 1][0]) == xi and steps[k - 1][3] == '0', f'{name} day {d} left side without right side')
            if sided[xi] == 1 and fl == 0:
                check(k + 1 < len(steps) and int(steps[k + 1][0]) == xi and steps[k + 1][3] == '1', f'{name} day {d} right side without left side')
            sec += ss
        for rest in (8, 12, 20):
            tot = 0
            for k, s in enumerate(steps):
                tot += int(s[1]); 
                if k < len(steps) - 1: tot += rest if int(s[2]) == -1 else int(s[2])
            (mins if name == 'plan' else jmins).append(tot / 60)
        if name == 'plan': n_train += 1
check(n_train == 1108, f'training days {n_train}')
print('training days', n_train, 'rest days', TOTAL - n_train)
print('session minutes (all rest settings): min %.1f max %.1f' % (min(mins), max(mins)))
print('jaw minutes: min %.1f max %.1f' % (min(jmins), max(jmins)))
print('sprite bytes (webp only): %.2f MB' % (total_bytes / 1048576))
asset_total = sum(os.path.getsize(os.path.join(r, f)) for r, _, fs in os.walk(A) for f in fs)
print('all assets: %.2f MB' % (asset_total / 1048576))
print('FAILS:', len(fails)); [print(' -', f) for f in fails[:30]]
sys.exit(1 if fails else 0)
