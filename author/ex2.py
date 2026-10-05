from build import *

# ---------- RUN CYCLES ----------
def runset(id,name,met,desc,tips,stride,knee,lean,lift,armf,armb,loop,lvl=1):
    def ph(nl,fl):
        # nl: near leg state, fl: far leg state each in ('fwd','mid_up','back','mid')
        pass
    A=dict(t=lean,tn=stride,sn=stride*0.6,tf=-stride,sf=-stride-knee,un=-armb,fn=-armb+55,uf=armf,ff=armf+65,lift=lift)
    B=dict(t=lean,tn=3,sn=0,tf=stride*0.9+10,sf=-knee*0.8,un=-armb*0.3,fn=40,uf=armf*0.4,ff=armf*0.4+75,lift=0)
    C=dict(t=lean,tf=stride,sf=stride*0.6,tn=-stride,sn=-stride-knee,uf=-armb,ff=-armb+55,un=armf,fn=armf+65,lift=lift)
    D=dict(t=lean,tf=3,sf=0,tn=stride*0.9+10,sn=-knee*0.8,uf=-armb*0.3,ff=40,un=armf*0.4,fn=armf*0.4+75,lift=0)
    E(id,name,"R",met,desc,tips,[P(**A),P(**B),P(**C),P(**D)],loop=loop,ease=0,lvl=lvl)

runset("walk","BRISK WALK",4.3,"Walk fast with long, relaxed strides and swinging arms. Breathe easily.",
       ["Look ahead, not down.","Roll from heel to toe on every step."],stride=24,knee=20,lean=2,lift=0,armf=25,armb=25,loop=1.1)
runset("jog","EASY JOG",8.0,"Jog at a comfortable pace where you could still hold a conversation.",
       ["Short, quick steps. Land under your hips.","Relax your hands and shoulders."],stride=34,knee=55,lean=6,lift=0.02,armf=50,armb=35,loop=0.8)
runset("run","STEADY RUN",9.8,"Run at a strong, steady pace with a slight forward lean and active arms.",
       ["Stay tall through the hips.","Breathe in a steady rhythm."],stride=38,knee=70,lean=8,lift=0.03,armf=55,armb=40,loop=0.7,lvl=2)
runset("sprint","SPRINT",12.0,"Run at a fast, controlled pace with a powerful arm drive. Fully recover afterwards.",
       ["Drive your elbows back hard.","Stay relaxed in the face and shoulders."],stride=48,knee=85,lean=16,lift=0.05,armf=70,armb=50,loop=0.5,lvl=3)

# ---------- DECOMPRESSION ----------
E("hang","DEAD HANG","D",3.5,
  "Grip the bar with straight arms and let your body hang long and relaxed. Let your spine decompress.",
  ["Relax your shoulders and legs.","Breathe slowly. Come down if your grip fails."],
  [P(t=0,un=173,fn=176,uf=-173,ff=-176,tn=1,sn=1,tf=-1,sf=-1,grab=0.15),
   P(t=1,un=173,fn=176,uf=-173,ff=-176,tn=5,sn=5,tf=3,sf=3,grab=0.15)], loop=3.0, front=True, bar=True, hold=True)

E("kneeraise","HANGING KNEE RAISE","C",5.0,
  "Hang from the bar and slowly lift your knees toward your chest, then lower with control.",
  ["Do not swing. Lift with your abs.","Keep your shoulders active."],
  [P(t=-2,un=180,fn=180,uf=180,ff=180,tn=2,sn=2,tf=2,sf=2,grab=0.15),
   P(t=-8,un=180,fn=180,uf=180,ff=180,tn=105,sn=10,tf=100,sf=8,grab=0.15)], loop=2.6, bar=True, lvl=3)

mat_prone=dict(fo=0,tn=-90,sn=-90,tf=-90,sf=-90)
E("cobra","COBRA STRETCH","D",2.5,
  "Lie face down, place your hands under your shoulders and gently press your chest up while your hips stay on the floor.",
  ["Keep your neck long; look slightly forward.","Stop if you feel any pain in your lower back."],
  [P(t=84,h=72,ra=(0.30,0.045,-1),la=(0.30,0.045,-1),**mat_prone),
   P(t=52,h=48,ra=(0.30,0.045,-1),la=(0.30,0.045,-1),**mat_prone),
   P(t=52,h=48,ra=(0.30,0.045,-1),la=(0.30,0.045,-1),**mat_prone)], loop=4.0, mat=True, hold=True)

E("child","CHILD'S POSE","D",2.0,
  "Sit back on your heels, reach your arms forward along the floor and let your back lengthen.",
  ["Breathe deep into your back.","Relax your forehead toward the floor."],
  [P(t=68,h=82,tn=82,sn=-90,tf=82,sf=-90,fo=0,ra=(0.54,0.03,-1),la=(0.54,0.03,-1)),
   P(t=74,h=88,tn=82,sn=-90,tf=82,sf=-90,fo=0,ra=(0.56,0.03,-1),la=(0.56,0.03,-1))], loop=4.0, mat=True, hold=True)

quad=dict(tn=0,sn=-90,tf=0,sf=-90,fo=0)
E("catcow","CAT-COW","D",2.5,
  "On hands and knees, slowly round your back while tucking your chin, then arch gently while looking forward.",
  ["Move one vertebra at a time.","Keep your hands under your shoulders."],
  [P(t=96,h=52,ra=(0.30,0.26,-1),la=(0.30,0.26,-1),**quad),
   P(t=80,h=112,ra=(0.30,0.26,-1),la=(0.30,0.26,-1),**quad)], loop=4.0, mat=True)

E("knees2chest","KNEES TO CHEST","D",2.2,
  "Lie on your back, hug your knees toward your chest and gently rock to release your lower back.",
  ["Keep your head and shoulders relaxed.","Breathe out as you pull your knees in."],
  [P(t=88,h=88,fo=0,tn=-90,sn=-90,tf=-90,sf=-90,un=-90,fn=-90),
   P(t=88,h=88,tn=150,sn=-20,tf=150,sf=-20,ra=(0.15,-0.19,-1),la=(0.15,-0.19,-1))], loop=4.0, mat=True, hold=True)

E("elongate","FULL-BODY STRETCH","D",2.0,
  "Lie on your back and reach your arms overhead and your toes away. Lengthen your whole body like a rubber band.",
  ["Press your lower back gently to the floor.","Inhale as you stretch, exhale as you relax."],
  [P(t=88,h=88,fo=0,tn=-90,sn=-90,tf=-90,sf=-90,un=-80,fn=-80),
   P(t=88,h=88,fo=20,tn=-90,sn=-90,tf=-90,sf=-90,un=95,fn=92)], loop=5.0, mat=True, hold=True)

if __name__=="__main__":
    import ex1
    preview(ids=["walk","jog","run","sprint","hang","kneeraise","cobra","child","catcow","knees2chest","elongate"],path="sheet2.png")
