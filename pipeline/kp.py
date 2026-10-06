import sys, os
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np, cv2
import mediapipe as mp
from PIL import Image
POSE = mp.solutions.pose.Pose(static_image_mode=True, model_complexity=1, enable_segmentation=False, min_detection_confidence=0.3)
# landmarks used: nose, shoulders, elbows, wrists, hips, knees, ankles, heels, foot index
IDX = [0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32]

def detect(rgba, scale=1.0):
    """rgba PIL (figure on transparent). returns (17,3) array x,y,visibility in pixels or None"""
    a = np.asarray(rgba).astype(np.float32)
    al = a[..., 3:4] / 255.0
    bg = np.full_like(a[..., :3], 205.0)
    rgb = (a[..., :3] * al + bg * (1 - al)).astype(np.uint8)
    h, w = rgb.shape[:2]
    pad = int(0.25 * max(h, w))
    canvas = cv2.copyMakeBorder(rgb, pad, pad, pad, pad, cv2.BORDER_CONSTANT, value=(205, 205, 205))
    res = POSE.process(canvas)
    if not res.pose_landmarks: return None
    lm = res.pose_landmarks.landmark
    H, W = canvas.shape[:2]
    out = np.array([[lm[i].x * W - pad, lm[i].y * H - pad, lm[i].visibility] for i in IDX], np.float32)
    return out
