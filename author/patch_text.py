"""Texts of the exercises whose drawing now comes from the RepDB illustrations (https://repdb.co).
Applied on top of exercises.json by export.py. Fields: n = English name, es = Spanish name, d = Spanish guide, t = Spanish tips."""
import json, os
P = {
 'march':     dict(n='BODYWEIGHT SQUAT', es='SENTADILLA LIBRE', p='W', m=5.0, sd=0,
                   d='De pie, con los pies al ancho de los hombros, lleva la cadera hacia atrás y abajo como si te sentaras, y vuelve a subir empujando el suelo.',
                   t=['Mantén el pecho arriba y la espalda recta.', 'Las rodillas siguen la dirección de los pies, sin juntarse.']),
 'armcirc':   dict(n='SHOULDER STRETCH', es='ESTIRAMIENTO DE HOMBRO', p='W', m=2.0, sd=1,
                   d='Cruza un brazo estirado por delante del pecho y sujétalo con el otro brazo, sin girar el torso.',
                   t=['Mantén el hombro abajo, lejos de la oreja.', 'Respira lento y estira sin rebotar.']),
 'buttkick':  dict(n='REVERSE LUNGE', es='ZANCADA HACIA ATRÁS', p='W', m=4.5, sd=1,
                   d='Da un paso largo hacia atrás y baja hasta que ambas rodillas formen unos 90 grados; empuja con el pie de delante para volver.',
                   t=['Torso erguido y la rodilla de delante sobre el tobillo.', 'Baja con control, sin dejar caer la rodilla de atrás al suelo.']),
 'elongate':  dict(n='SUPINE SPINAL TWIST', es='TORSIÓN DE COLUMNA ACOSTADO', p='D', m=2.0, sd=1,
                   d='Acostado boca arriba con los brazos abiertos, deja caer las rodillas dobladas hacia un lado mientras los hombros siguen en el suelo.',
                   t=['Mantén ambos hombros pegados al suelo.', 'Relaja la espalda y respira profundo en cada lado.']),
 'wallangel': dict(n='WALL PUSH-UPS', es='FLEXIONES EN PARED', p='C', m=3.5, sd=0,
                   d='De pie frente a una pared, apoya las manos a la altura del pecho y flexiona los codos para acercar el pecho a la pared; empuja para volver.',
                   t=['Cuerpo recto de la cabeza a los talones.', 'Codos a unos 45 grados del cuerpo, sin abrirlos.']),
 'wallstand': dict(n='MOUNTAIN POSE', es='POSTURA DE LA MONTAÑA', p='C', m=2.0, sd=0,
                   d='De pie, con los pies juntos o al ancho de la cadera, estírate hacia arriba con los hombros relajados y la mirada al frente. Es la postura base para alinear la espalda.',
                   t=['Imagina un hilo que tira de la coronilla hacia el techo.', 'Reparte el peso por igual en ambos pies.']),
 'reach':     dict(n='STANDING FORWARD FOLD', es='FLEXIÓN DE PIE HACIA ADELANTE', p='C', m=2.0, sd=0,
                   d='De pie, dobla la cadera y deja caer el torso hacia delante, con las manos hacia el suelo o los tobillos, soltando cuello y espalda.',
                   t=['Dobla las rodillas lo necesario para no forzar la espalda.', 'Deja la cabeza pesada y respira lento.']),
 'legswing':  dict(d='De pie, balancea una pierna recta hacia delante y hacia atrás con control. Apóyate en una pared si necesitas equilibrio.',
                   t=['Mantén el torso erguido, sin inclinarte.', 'Aumenta el balanceo poco a poco, sin forzar.']),
 'calf':      dict(d='En posición de zancada, con la pierna de atrás estirada y el talón apoyado, inclina el cuerpo hacia delante para estirar la pantorrilla.',
                   t=['Mantén el talón de atrás pegado al suelo.', 'Estira sin rebotar.']),
 'chestopen': dict(d='De pie, con los codos doblados a la altura de los hombros, lleva los codos hacia atrás para abrir el pecho, como en el estiramiento en el marco de una puerta.',
                   t=['Mantén los hombros abajo y el cuello relajado.', 'No arquees la zona lumbar.']),
 'highknee':  dict(d='Corre en el sitio llevando cada rodilla a la altura de la cadera, moviendo los brazos con ritmo.',
                   t=['Mantente sobre la punta de los pies.', 'Sube la rodilla, no la lances hacia delante.']),
}
def apply(path):
    ex = json.load(open(path))
    for e in ex:
        for k, v in P.get(e['id'], {}).items():
            e[{'n': 'n', 'es': 'es', 'd': 'd', 't': 't', 'p': 'p', 'm': 'm', 'sd': 'sd'}[k]] = v
    json.dump(ex, open(path, 'w'), ensure_ascii=False, separators=(',', ':'))
    return len(P)
