from build import *
import math
from figure import fk, extent, default

def plank_t(arm_u, arm_f):
    best=None
    for t10 in range(500,900):
        t=t10/10
        Q=default(dict(t=t,tn=-t,sn=-t,un=arm_u,fn=arm_f))
        pts=fk(Q)
        ext=extent(Q,pts)
        toe=max(y+r for x,y,r in ext if x<pts["H"][0])  # leg side
        hand=max(pts["En"][1]+0.03,pts["Wn"][1]+0.02,pts["Hdn"][1]+0.02)
        d=abs(toe-hand)
        if best is None or d<best[0]: best=(d,t)
    return best[1]
TP = plank_t(0,90)
TH = plank_t(0,0)
print("plank t forearm",TP,"hands",TH)

E("plank","FOREARM PLANK","C",3.8,
  "Rest on your forearms and toes with your body in one straight line from head to heels. Squeeze your abs and glutes.",
  ["Do not let your hips sag or rise.","Breathe steadily and keep your neck neutral."],
  [P(t=TP,h=TP-8,tn=-TP,sn=-TP,tf=-TP,sf=-TP,un=0,fn=90),
   P(t=TP+1.5,h=TP-6,tn=-TP-1.5,sn=-TP-1.5,tf=-TP-1.5,sf=-TP-1.5,un=0,fn=90)], loop=3.0, mat=True, hold=True, lvl=1)

E("mclimb","MOUNTAIN CLIMBERS","I",8.0,
  "From a high plank, drive your knees toward your chest one after the other at a steady pace.",
  ["Keep your hips level and shoulders over your hands.","Move fast but stay in control."],
  [P(t=TH,h=TH-8,tn=-TH,sn=-TH,tf=62,sf=-50,un=0,fn=0,uf=0,ff=0),
   P(t=TH,h=TH-8,tf=-TH,sf=-TH,tn=62,sn=-50,un=0,fn=0,uf=0,ff=0)], loop=0.7, mat=True, lvl=2)

E("burpee","BURPEES","I",10.0,
  "Squat down, kick your feet back to a plank, return to a squat and jump up with your arms overhead.",
  ["Keep your core tight in the plank.","Land softly and reset your breathing."],
  [P(t=2,un=6,fn=6,uf=6,ff=6),
   P(t=75,h=60,tn=88,sn=-35,tf=88,sf=-35,ra=(0.33,0.20,-1),la=(0.33,0.20,-1)),
   P(t=TH,h=TH-8,tn=-TH,sn=-TH,tf=-TH,sf=-TH,un=0,fn=0,uf=0,ff=0),
   P(t=75,h=60,tn=88,sn=-35,tf=88,sf=-35,ra=(0.33,0.20,-1),la=(0.33,0.20,-1)),
   P(t=0,un=172,fn=172,uf=172,ff=172,tn=2,sn=2,tf=2,sf=2,lift=0.12)], loop=3.0, anchor="hip", lvl=4)

# ---- core / posture ----
E("wallangel","WALL ANGELS","C",2.8,
  "Stand with your back against a wall and slide your arms up and down like a snow angel, keeping contact with the wall.",
  ["Keep your lower back, head and arms touching the wall.","Move slowly and do not shrug your shoulders."],
  [P(t=0,un=92,fn=175,uf=-92,ff=-175,tn=5,tf=-5),
   P(t=0,un=172,fn=176,uf=-172,ff=-176,tn=5,tf=-5)], loop=3.0, front=True)

E("wallstand","WALL POSTURE HOLD","C",2.0,
  "Stand with your heels, hips, shoulders and head against the wall. Gently tuck your chin and grow tall.",
  ["Imagine a string pulling the top of your head upward.","Keep your ribs down and breathe slowly."],
  [P(t=5,h=14,hf=0.05,un=-5,fn=-5),P(t=0,h=0,hf=-0.005,un=-3,fn=-3),P(t=0,h=0,hf=-0.005,un=-3,fn=-3)],
  loop=5.0, wall=-0.12, hold=True)

E("reach","OVERHEAD REACH","C",2.5,
  "Raise your arms overhead and rise onto your toes, stretching as tall as you can. Lower slowly and repeat.",
  ["Reach through your fingertips.","Keep your core tight to avoid arching your back."],
  [P(t=0,un=-5,fn=-5),P(t=-2,un=176,fn=176,uf=176,ff=176,fo=52,lift=0)], loop=3.2, anchor="foot")

E("deadbug","DEAD BUG","C",3.5,
  "Lie on your back with arms up and knees bent. Lower the opposite arm and leg slowly, keeping your lower back pressed down.",
  ["Move slowly. Exhale as you extend.","Do not let your back arch off the floor."],
  [P(t=88,h=88,un=180,fn=180,uf=180,ff=180,tn=180,sn=-90,tf=180,sf=-90),
   P(t=88,h=88,un=180,fn=180,uf=90,ff=90,tn=-92,sn=-92,tf=180,sf=-90,fo=0),
   P(t=88,h=88,un=180,fn=180,uf=180,ff=180,tn=180,sn=-90,tf=180,sf=-90),
   P(t=88,h=88,un=90,fn=90,uf=180,ff=180,tf=-92,sf=-92,tn=180,sn=-90,fo=0,fof=0)], loop=4.0, mat=True, lvl=2)

E("birddog","BIRD DOG","C",3.0,
  "On hands and knees, extend one arm forward and the opposite leg back. Hold briefly, then switch sides.",
  ["Keep your hips level, as if balancing a glass of water.","Reach long instead of high."],
  [P(t=88,h=80,ra=(0.30,0.26,-1),la=(0.30,0.26,-1),tn=0,sn=-90,tf=0,sf=-90,fo=0),
   P(t=88,h=84,un=90,fn=90,la=(0.30,0.26,-1),tn=0,sn=-90,tf=-92,sf=-92,fo=0,fof=0),
   P(t=88,h=80,ra=(0.30,0.26,-1),la=(0.30,0.26,-1),tn=0,sn=-90,tf=0,sf=-90,fo=0),
   P(t=88,h=84,uf=90,ff=90,ra=(0.30,0.26,-1),tf=0,sf=-90,tn=-92,sn=-92,fo=0,fof=0)], loop=6.0, mat=True, lvl=2)

E("superman","SUPERMAN","C",3.0,
  "Lie face down with arms forward. Lift your arms, chest and legs a few centimeters off the floor and hold.",
  ["Look at the floor to keep your neck neutral.","Squeeze your glutes and lower back gently."],
  [P(t=86,h=80,un=95,fn=95,uf=95,ff=95,fo=0,tn=-90,sn=-90,tf=-90,sf=-90),
   P(t=76,h=72,un=112,fn=112,uf=112,ff=112,fo=0,tn=-100,sn=-100,tf=-100,sf=-100),
   P(t=76,h=72,un=112,fn=112,uf=112,ff=112,fo=0,tn=-100,sn=-100,tf=-100,sf=-100)], loop=4.0, mat=True, hold=True)

E("bridge","GLUTE BRIDGE","C",3.0,
  "Lie on your back with knees bent and feet flat. Lift your hips until your body forms a straight line from shoulders to knees.",
  ["Push through your heels.","Squeeze your glutes at the top."],
  [P(t=88,h=88,un=-90,fn=-90,uf=-90,ff=-90,fo=90,rl=(-0.30,0.07,-1),ll=(-0.30,0.07,-1)),
   P(t=158,h=120,un=-90,fn=-90,uf=-90,ff=-90,fo=90,rl=(-0.26,0.30,-1),ll=(-0.26,0.30,-1))], loop=3.5, mat=True)

# ---- flexibility ----
E("calf","CALF STRETCH","F",2.3,
  "Step one leg back with the heel on the floor and press your hands against a wall. Bend the front knee to feel the stretch.",
  ["Keep the back heel on the floor.","Keep your back leg straight."],
  [P(t=8,un=84,fn=84,uf=84,ff=84,tn=48,sn=-14,fo=104,tf=-33,sf=-33,fof=123),
   P(t=14,un=84,fn=84,uf=84,ff=84,tn=52,sn=-16,fo=106,tf=-36,sf=-36,fof=126)], loop=4.0, sided=True, wall=0.42, anchor="foot", hold=True)

E("quadstretch","QUAD STRETCH","F",2.3,
  "Stand on one leg, pull your other heel toward your glute and keep your knees together. Hold for balance.",
  ["Keep your knees close together.","Stand tall and do not arch your back."],
  [P(t=0,un=-5,fn=-5,uf=40,ff=40),
   P(t=-4,tn=-6,sn=-160,ra=(-0.10,0.03,-1),uf=50,ff=50),
   P(t=-4,tn=-6,sn=-160,ra=(-0.10,0.03,-1),uf=50,ff=50)], loop=5.0, sided=True, hold=True)

E("hipflex","HIP FLEXOR STRETCH","F",2.3,
  "Kneel in a lunge, tuck your pelvis and gently push your hips forward. Raise your arms for a deeper stretch.",
  ["Squeeze the glute of the back leg.","Keep your chest tall."],
  [P(t=0,tn=85,sn=0,tf=-5,sf=-90,fof=0,un=-20,fn=40,uf=-20,ff=40),
   P(t=-6,tn=85,sn=0,tf=-5,sf=-90,fof=0,un=172,fn=172,uf=172,ff=172),
   P(t=-6,tn=85,sn=0,tf=-5,sf=-90,fof=0,un=172,fn=172,uf=172,ff=172)], loop=5.0, sided=True, mat=True, hold=True)

E("hamfold","SEATED FORWARD FOLD","F",2.0,
  "Sit with your legs straight, then fold forward from the hips and reach toward your toes. Keep your back long.",
  ["Hinge from the hips, not the back.","Relax your neck and breathe out as you fold."],
  [P(t=0,h=0,tn=90,sn=90,tf=90,sf=90,un=70,fn=75,uf=70,ff=75),
   P(t=68,h=58,tn=90,sn=90,tf=90,sf=90,un=80,fn=84,uf=80,ff=84),
   P(t=68,h=58,tn=90,sn=90,tf=90,sf=90,un=80,fn=84,uf=80,ff=84)], loop=5.0, mat=True, hold=True)

E("sidebend","STANDING SIDE BEND","F",2.3,
  "Stand with feet hip-width apart, reach both arms overhead and bend gently to one side, then the other.",
  ["Keep both feet flat on the floor.","Lengthen up before you bend over."],
  [P(t=-20,un=200,fn=200,uf=200,ff=200,tn=6,tf=-6),
   P(t=20,un=160,fn=160,uf=160,ff=160,tn=6,tf=-6)], loop=5.0, front=True)

E("chestopen","CHEST OPENER","F",2.0,
  "Stand tall, bring your straight arms behind your back and lift them gently while opening your chest.",
  ["Pull your shoulder blades toward each other.","Keep your chin level."],
  [P(t=0,un=-4,fn=-4,uf=-4,ff=-4),
   P(t=-5,h=-10,un=-118,fn=-118,uf=-124,ff=-124),
   P(t=-5,h=-10,un=-118,fn=-118,uf=-124,ff=-124)], loop=5.0, hold=True)

# ---- jaw / neck (bust) ----
B=dict(bust=True, smax=3.4)
E("chintuck","CHIN TUCK","J",1.5,
  "Sit or stand tall. Glide your head straight back, as if making a gentle double chin, and hold. Keep your eyes level.",
  ["Do not tilt your head down.","Feel the stretch at the base of your skull."],
  [P(t=0,h=2,hf=0.016),P(t=0,h=0,hf=-0.012),P(t=0,h=0,hf=-0.012)], loop=4.5, hold=True, **B)

E("jawopen","CONTROLLED JAW OPENING","J",1.5,
  "Place your tongue on the roof of your mouth and open slowly, only as far as is comfortable. Close with control.",
  ["Open in a straight line with no clicking.","Stop if you feel pain or hear popping."],
  [P(t=0,jaw=0),P(t=0,jaw=22),P(t=0,jaw=0)], loop=4.5, **B)

E("jawresist","RESISTED JAW OPENING","J",1.5,
  "Place your fist under your chin and open your mouth slowly against gentle resistance. Hold, then release.",
  ["Use light pressure only.","Keep your head still and your shoulders relaxed."],
  [P(t=0,jaw=2,ra=(0.075,-0.315,-1)),P(t=0,jaw=12,ra=(0.075,-0.315,-1)),P(t=0,jaw=12,ra=(0.075,-0.315,-1))], loop=4.5, hold=True, **B)

E("necktilt","NECK SIDE STRETCH","J",1.5,
  "Tilt your ear toward your shoulder and let the weight of your head stretch the side of your neck. Switch sides.",
  ["Keep your opposite shoulder down.","Do not pull with your hand."],
  [P(t=0,h=-16,un=4,fn=4,uf=-4,ff=-4),P(t=0,h=16,un=4,fn=4,uf=-4,ff=-4)], loop=6.0, front=True, **B)

E("neckflex","NECK FLEXION STRETCH","J",1.5,
  "Slowly lower your chin toward your chest and feel a gentle stretch along the back of your neck. Return slowly.",
  ["Keep your shoulders relaxed.","Do not bounce or force the stretch."],
  [P(t=0,h=0,hf=0),P(t=0,h=40,hf=0.008),P(t=0,h=40,hf=0.008)], loop=5.0, hold=True, **B)

E("tonguepos","TONGUE POSTURE HOLD","J",1.3,
  "Rest your whole tongue lightly on the roof of your mouth, teeth almost touching and lips closed. Breathe through your nose.",
  ["Do not press hard. Keep it light.","Stay tall with your head balanced over your shoulders."],
  [P(t=0,h=0,hf=0.0),P(t=0,h=0.5,hf=0.002)], loop=5.0, hold=True, **B)

E("jawmassage","MASSETER MASSAGE","J",1.3,
  "With two fingers, make slow circles on the muscle of your cheek near the jaw. Use gentle pressure.",
  ["Breathe out as you massage.","Keep your jaw relaxed and teeth slightly apart."],
  [P(t=0,jaw=4,ra=(0.075,-0.355,-1)),P(t=0,jaw=4,ra=(0.075,-0.330,-1)),P(t=0,jaw=4,ra=(0.050,-0.330,-1)),P(t=0,jaw=4,ra=(0.050,-0.355,-1))], loop=4.0, ease=0, **B)

if __name__=="__main__":
    import ex1, ex2
    preview(ids=["plank","mclimb","burpee","wallangel","wallstand","reach","deadbug","birddog","superman","bridge"],path="sheet3.png")
    preview(ids=["calf","quadstretch","hipflex","hamfold","sidebend","chestopen","chintuck","jawopen","jawresist","necktilt","neckflex","tonguepos","jawmassage"],path="sheet4.png")
