from build import *

# ---------- WARM-UP ----------
E("march","MARCH IN PLACE","W",3.5,
  "Stand tall and lift your knees alternately to hip height, swinging the opposite arm forward.",
  ["Keep your chest up and shoulders relaxed.","Land softly on the ball of each foot."],
  [P(t=3,tn=85,sn=0,tf=0,sf=0,un=-30,fn=20,uf=45,ff=115),
   P(t=3,tf=85,sf=0,tn=0,sn=0,un=45,fn=115,uf=-30,ff=20)], loop=1.0, ease=1)

arm=lambda a,lag=30: dict(un=a,fn=a,uf=a-lag,ff=a-lag)
E("armcirc","ARM CIRCLES","W",3.0,
  "Stand tall and swing both straight arms in big, smooth circles. Reverse the direction halfway.",
  ["Move from the shoulders, not the wrists.","Keep your neck long and ribs down."],
  [P(t=0,**arm(0)),P(t=0,**arm(90)),P(t=0,**arm(180)),P(t=0,**arm(270))], loop=2.0, ease=0)

E("jj","JUMPING JACKS","W",8.0,
  "Jump your feet apart while raising your arms overhead, then jump back to the start.",
  ["Land softly with soft knees.","Breathe steadily and keep a light rhythm."],
  [P(t=0,un=6,uf=-6,tn=2,tf=-2),
   P(t=0,un=165,fn=170,uf=-165,ff=-170,tn=24,tf=-24,lift=0.05)], loop=0.9, front=True, ax=0.5)

E("legswing","LEG SWINGS","W",2.8,
  "Hold a wall for balance and swing one straight leg forward and back in a relaxed, controlled arc.",
  ["Keep your torso tall; do not lean.","Increase the swing slowly, never force it."],
  [P(t=0,un=88,fn=92,uf=88,ff=92,tn=-28,sn=-28,tf=0,sf=0),
   P(t=0,un=88,fn=92,uf=88,ff=92,tn=42,sn=42,tf=0,sf=0)], loop=1.8, sided=True, wall=0.36, anchor="hip")

E("highknee","HIGH KNEES","I",9.0,
  "Run in place driving each knee up to hip height while pumping your arms.",
  ["Stay on the balls of your feet.","Drive the knee up, not forward."],
  [P(t=6,tn=100,sn=-8,tf=0,sf=0,un=-40,fn=10,uf=55,ff=120,lift=0.03),
   P(t=6,tf=100,sf=-8,tn=0,sn=0,un=55,fn=120,uf=-40,ff=10,lift=0.03)], loop=0.55, ease=1, lvl=2)

E("buttkick","BUTT KICKS","W",7.0,
  "Jog in place and kick your heels up toward your glutes with each step.",
  ["Keep your knees pointing down.","Swing your arms like a light jog."],
  [P(t=4,tn=-8,sn=-150,tf=0,sf=0,un=-35,fn=15,uf=45,ff=115,lift=0.02),
   P(t=4,tf=-8,sf=-150,tn=0,sn=0,un=45,fn=115,uf=-35,ff=15,lift=0.02)], loop=0.7, ease=1)

# ---------- IMPACT ----------
E("rope","JUMP ROPE","I",11.0,
  "Jump with small, quick hops, turning the rope with your wrists and keeping your elbows close to your body.",
  ["Jump just high enough to clear the rope.","Land softly on the balls of your feet."],
  [P(t=2,un=8,fn=75,uf=8,ff=75,tn=2,sn=2,tf=2,sf=2,rope=0),
   P(t=2,un=8,fn=75,uf=8,ff=75,tn=6,sn=-14,tf=6,sf=-14,rope=90,lift=0.04),
   P(t=2,un=8,fn=75,uf=8,ff=75,tn=6,sn=-14,tf=6,sf=-14,rope=180,lift=0.06),
   P(t=2,un=8,fn=75,uf=8,ff=75,tn=2,sn=2,tf=2,sf=2,rope=270)], loop=0.9, rope=True, ease=1)

squat=dict(t=42,tn=72,sn=-42,tf=72,sf=-42)
E("squatjump","SQUAT JUMP","I",9.0,
  "Sink into a squat, then explode upward into a jump, swinging your arms up. Land softly into the next squat.",
  ["Keep your chest up and knees over your toes.","Absorb the landing by bending your knees."],
  [P(**squat,un=-55,fn=-55,uf=-55,ff=-55),
   P(t=0,tn=2,sn=2,tf=2,sf=2,un=170,fn=170,uf=170,ff=170,lift=0.12),
   P(t=26,tn=46,sn=-20,tf=46,sf=-20,un=30,fn=30,uf=30,ff=30)], loop=1.6, anchor="foot", lvl=2)

E("lungejump","LUNGE JUMPS","I",10.0,
  "From a lunge, jump and switch legs in the air, landing softly in a lunge on the other side.",
  ["Keep your torso tall and your front knee over your ankle.","Start with small jumps and build height over time."],
  [P(t=3,tn=70,sn=0,tf=-25,sf=-80,un=60,fn=110,uf=-40,ff=0),
   P(t=3,tn=25,sn=-25,tf=-20,sf=-45,un=20,fn=80,uf=20,ff=80,lift=0.10),
   P(t=3,tf=70,sf=0,tn=-25,sn=-80,un=-40,fn=0,uf=60,ff=110),
   P(t=3,tn=25,sn=-25,tf=-20,sf=-45,un=20,fn=80,uf=20,ff=80,lift=0.10)], loop=1.8, lvl=3)

if __name__=="__main__":
    preview(path="sheet1.png")
