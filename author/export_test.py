import json
ex=json.load(open("/home/claude/grow/app/src/main/assets/exercises.json"))
out=[]
for e in ex:
    mat=e["mat"] or [0,0]
    out.append("EX|%s|%s|%d|%d|%f|%f|%d|%f|%f|%d|%f|%f|%d|%d"%(e["id"],e["n"],e["fr"],e["bu"],e["sc"],e["lp"],e["ez"],e["bar"],e["wall"],1 if e["mat"] else 0,mat[0],mat[1],e["rp"],len(e["ps"])))
    for p in e["ps"]:
        out.append("P|"+",".join(str(v) for v in p))
open("/tmp/ex_test.txt","w").write("\n".join(out))
print(len(ex))
