import sys, json, math, os, datetime
sys.path.insert(0,'.')
import ex1, ex2, ex3
from build import EX, prepare
from figure import FLOOR

OUT = "/home/claude/grow/app/src/main/assets"
os.makedirs(OUT, exist_ok=True)

def r(v, n=3): return round(float(v), n)

exs = []
index = {}
for e in EX:
    out, s, pr = prepare(e)
    poses = []
    for Q in out:
        poses.append([r(Q["hip"][0]), r(Q["hip"][1]), r(Q["t"],1), r(Q["h"],1), r(Q["hf"]*s,4),
                      r(Q["un"],1), r(Q["fn"],1), r(Q["uf"],1), r(Q["ff"],1),
                      r(Q["tn"],1), r(Q["sn"],1), r(Q["tf"],1), r(Q["sf"],1),
                      r(Q["jaw"],1), r(Q["fo"],1), r(Q["fof"],1), (-999 if Q.get("rope") is None else r(Q["rope"],1))])
    d = dict(id=e["id"], n=e["name"], d=e["desc"], t=e["tips"], p=e["pillar"], m=e["met"], sd=int(e["sided"]), lv=e["lvl"],
             lp=e["loop"], ez=e["ease"], fr=int(e["front"]), bu=int(e["bust"]), sc=r(s,3),
             bar=(r(pr["bar"],3) if "bar" in pr else -1), wall=(r(pr["wall"],3) if "wall" in pr else -1),
             mat=(pr["mat"] if "mat" in pr else None), rp=int(e["rope"]), ps=poses)
    index[e["id"]] = len(exs)
    exs.append(d)
json.dump(exs, open(os.path.join(OUT,"exercises.json"),"w"), separators=(",",":"), ensure_ascii=False)
print("exercises", len(exs), "bytes", os.path.getsize(os.path.join(OUT,"exercises.json")))

# ------------------------------------------------------------------ PLAN
START = datetime.date(2026,10,5); END = datetime.date(2030,4,18)
TOTAL = (END-START).days+1
def phase(d):
    return 0 if d<=90 else 1 if d<=365 else 2 if d<=730 else 3 if d<=1100 else 4

LV = {e["id"]:e["lvl"] for e in EX}
SIDED = {e["id"]:e["sided"] for e in EX}
HOLD = {e["id"]:e["hold"] for e in EX}
def pool(ids, ph):
    return [i for i in ids if LV[i] <= ph+1]

WARM=["march","armcirc","buttkick","jj","legswing"]
IMPACT=["jj","rope","squatjump","highknee","lungejump","mclimb","burpee"]
CORE=["wallstand","plank","wallangel","deadbug","birddog","superman","bridge","reach","kneeraise"]
DECOMP=["hang","cobra","child","catcow","knees2chest","elongate"]
FLEX=["calf","quadstretch","hipflex","hamfold","sidebend","chestopen"]

COUNTS = {
 0: dict(W=[4,4,4,5,4], I=[3,4,4,5,4], C=[3,4,5,5,4], D=[3,3,3,4,3], F=[3,3,4,4,3]),
 1: dict(W=[3,3,3,4,3], F=[3,3,4,4,3], D=[2,2,3,3,2]),
 2: dict(W=[4,4,4,5,4], C=[3,4,5,6,5], D=[3,3,4,4,3], F=[3,4,4,5,4]),
 3: dict(W=[4,4,4,5,4], I=[3,4,4,5,4], D=[4,4,4,5,4], F=[3,3,4,4,3]),
 4: dict(W=[3,3,3,4,3], C=[3,3,4,5,4], D=[2,2,3,3,2]),
 6: dict(D=[4,4,4,5,4], F=[3,3,4,4,3], C=[2,2,3,3,2]),
}
ORDER = {0:"WICDF",1:"WRFD",2:"WCDF",3:"WIDF",4:"WRCD",6:"DFCX"}
PH_START=[1,91,366,731,1101,TOTAL+1]
W_RAMP=[(20,30),(30,40),(35,45),(40,50),(35,40)]
def work(d, ph, deload):
    lo,hi=W_RAMP[ph]
    a=PH_START[ph]; b=PH_START[ph+1]
    prog=(d-a)/max(1,(b-a-1))
    w=lo+(hi-lo)*prog
    if deload: w*=0.8
    return int(round(w/5.0)*5)

def pick(lst, n, seed):
    out=[]; k=seed
    for _ in range(n):
        out.append(lst[k%len(lst)]); k+=1
    seen=set(); res=[]
    for x in out:
        if x not in seen: res.append(x); seen.add(x)
    return res

def steps_for(ids, w, ph):
    res=[]
    for i in ids:
        if SIDED[i]:
            res.append((i,w,5,0)); res.append((i,w,-1,1))
        else:
            res.append((i,w,-1,0))
    return res

def runblock(ph, deload):
    if ph==0: s=[("walk",60),("jog",40),("walk",60)]*3
    elif ph==1: s=[("walk",60)]+[("jog",60),("walk",60)]*4
    elif ph==2: s=[("jog",180)]+[("run",60),("jog",60)]*5
    elif ph==3: s=[("jog",180)]+[("run",90),("jog",45)]*4+[("sprint",20),("walk",40)]*3
    else: s=[("jog",180)]+[("run",90),("jog",60)]*4
    if deload: s=s[:len(s)-2]
    out=[(a,b,0,0) for a,b in s]
    out[-1]=(out[-1][0],out[-1][1],-1,0)
    return out

def dur(steps, rest=10):
    t=0
    for k,(i,sec,rs,fl,*_) in enumerate(steps):
        t+=sec
        if k<len(steps)-1: t += (rest if rs==-1 else rs)
    return t

def session(d, date):
    wd = date.weekday()
    if wd==5: return None
    ph = phase(d); week=(d-1)//7; deload = (week%4==3 and week>0)
    w = work(d, ph, deload)
    seed = week*3 + wd
    pools = dict(W=pool(WARM,ph), I=pool(IMPACT,ph), C=pool(CORE,ph), D=pool(DECOMP,ph), F=pool(FLEX,ph))
    off = dict(W=0,I=1,C=2,D=3,F=4)
    steps=[]
    for b in ORDER[wd]:
        blk=[]
        if b=="R": blk=runblock(ph,deload)
        elif b=="X": blk=[("walk",180 if ph<2 else 300,-1,0)]
        else:
            n = COUNTS[wd][b][ph]
            if deload and b!="W": n=max(1,n-1)
            ids = pick(pools[b], n, seed+off[b])
            if b=="W":
                for i in ids:
                    if SIDED[i]: blk += [(i,w,5,0),(i,w,-1,1)]
                    else: blk.append((i,w,-1,0))
            else:
                blk = steps_for(ids,w,ph)
        tag = "W" if b=="W" else ("R" if b in "RX" else b)
        blk=[(a,b_,c,d_,tag) for (a,b_,c,d_) in blk]
        if b=="X": steps = blk + steps
        else: steps += blk
    return steps, ph, deload

# step encoding: exIdx:sec:rest:flag
lines=[]; stats={}
for d in range(1,TOTAL+1):
    date=START+datetime.timedelta(days=d-1)
    res=session(d,date)
    if res is None:
        lines.append("R"); continue
    steps,ph,dl=res
    enc=",".join(f"{index[i]}:{sec}:{rs}:{fl}:{tg}" for (i,sec,rs,fl,tg) in steps)
    lines.append(enc)
    stats.setdefault((ph,date.weekday()),[]).append(dur(steps)/60.0)
open(os.path.join(OUT,"plan.txt"),"w").write("\n".join(lines))
print("plan lines",len(lines),"bytes",os.path.getsize(os.path.join(OUT,"plan.txt")))
for k in sorted(stats):
    v=stats[k]; print("phase",k[0]+1,"wd",k[1],"min %.1f avg %.1f max %.1f"%(min(v),sum(v)/len(v),max(v)))

# ----------------------------------------------------------------- JAW PLAN
jlines=[]; jstats=[]
for d in range(1,TOTAL+1):
    date=START+datetime.timedelta(days=d-1)
    if date.weekday()==5: jlines.append("R"); continue
    ph=phase(d); week=(d-1)//7; wd=date.weekday()
    lo,hi=[(35,50),(45,55),(50,60),(55,65),(50,60)][ph]
    a_=PH_START[ph]; b_=PH_START[ph+1]
    w=int(round((lo+(hi-lo)*(d-a_)/max(1,(b_-a_-1)))/5.0)*5)
    main_all=["chintuck","jawopen","tonguepos","jawresist"]
    main=[x for x in main_all if (x!="jawresist" or ph>=1)]
    nmain=[3,3,4,4,4][ph]
    mm=pick(main,nmain,week*2+wd)
    warm=pick(["neckflex","necktilt"],2 if ph>=0 else 1,wd+week)
    ids=warm+mm+["jawmassage"]
    if ph==3: ids=ids+["tonguepos"] if ids[-2]!="tonguepos" else ids+["chintuck"]
    steps=[(i,w,-1,0,"J") for i in ids]
    enc=",".join(f"{index[i]}:{sec}:{rs}:{fl}:{tg}" for (i,sec,rs,fl,tg) in steps)
    jlines.append(enc); jstats.append((ph,dur(steps,8)/60.0))
open(os.path.join(OUT,"jaw.txt"),"w").write("\n".join(jlines))
for ph in range(5):
    v=[m for p,m in jstats if p==ph]; print("jaw phase",ph+1,"min %.1f max %.1f"%(min(v),max(v)))
print("total days",TOTAL,"training days",sum(1 for l in lines if l!="R"))
