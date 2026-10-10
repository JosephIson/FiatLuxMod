from geo import Material

GOLD = Material("gold", (236, 190, 66), "gold", accent=(255, 236, 150))
GOLD_DARK = Material("gold_dark", (178, 128, 38), "gold")
GOLD_GLOW = Material("gold_glow", (255, 214, 96), "gold", emissive=True)
IVORY = Material("ivory", (238, 228, 204), "plain")
IVORY_WRAP = Material("ivory_wrap", (214, 200, 170), "cloth")
CRYSTAL = Material("crystal", (190, 236, 255), "crystal", alpha=150)
CRYSTAL_CORE = Material("crystal_core", (236, 252, 255), "crystal", emissive=True, alpha=190)
SAPPHIRE = Material("sapphire", (64, 120, 255), "crystal", emissive=True)
HOLY_FIRE = Material("holy_fire", (255, 168, 40), "fire", emissive=True, alpha=210, accent=(255, 90, 20))
HOLY_FIRE_WHITE = Material("holy_fire_white", (255, 236, 150), "fire", emissive=True, alpha=200, accent=(255, 170, 60))
RUNE_GOLD = Material("rune_gold", (205, 160, 50), "rune", emissive=True, accent=(255, 248, 200))

SKIN = Material("skin", (236, 206, 180), "skin")
SKIN_SHADOW = Material("skin_shadow", (214, 180, 154), "skin")
HAIR = Material("hair", (232, 206, 140), "hair")
ROBE = Material("robe", (246, 244, 236), "cloth", accent=(232, 190, 80))
ROBE_SHADE = Material("robe_shade", (222, 220, 214), "cloth", accent=(232, 190, 80))
ROBE_TRIM = Material("robe_trim", (210, 168, 60), "cloth", accent=(255, 230, 140))
FEATHER = Material("feather", (250, 250, 246), "feather", accent=(214, 206, 186))
FEATHER_GOLD = Material("feather_gold", (246, 226, 168), "feather", accent=(200, 160, 80))
FEATHER_SHADE = Material("feather_shade", (226, 228, 232), "feather", accent=(190, 188, 180))
EYE_GLOW = Material("eye_glow", (255, 230, 120), "plain", emissive=True)
HALO = Material("halo", (255, 226, 110), "gold", emissive=True, accent=(255, 255, 220))
LEATHER = Material("leather", (150, 104, 62), "cloth")

# --- scriptural palette ------------------------------------------------------------
LINEN = Material("linen", (244, 242, 232), "cloth", accent=(226, 196, 98))           # Dan 10:5
UPHAZ_GOLD = Material("uphaz_gold", (246, 196, 60), "gold", emissive=True, accent=(255, 240, 170))
BERYL = Material("beryl", (214, 226, 150), "crystal", emissive=False)                 # Dan 10:6, Ezek 1:16
BERYL_GLOW = Material("beryl_glow", (226, 236, 160), "crystal", emissive=True)
LIGHTNING = Material("lightning", (232, 242, 255), "rune", emissive=True, accent=(160, 200, 255))  # face like lightning
TORCH_EYE = Material("torch_eye", (255, 150, 40), "fire", emissive=True, accent=(255, 70, 10))     # eyes like lamps of fire
BRONZE = Material("bronze", (196, 128, 66), "metal", accent=(240, 190, 120))           # burnished bronze
BRONZE_GLOW = Material("bronze_glow", (226, 150, 80), "metal", emissive=True)
SERAPH_FEATHER = Material("seraph_feather", (255, 196, 92), "feather", emissive=True, accent=(255, 110, 30))  # "burning ones"
SERAPH_SHADE = Material("seraph_shade", (255, 150, 60), "feather", emissive=True, accent=(220, 70, 20))
SERAPH_CORE = Material("seraph_core", (255, 240, 200), "feather", emissive=True, accent=(255, 180, 80))
EMBER = Material("ember", (255, 96, 24), "fire", emissive=True, accent=(160, 30, 10))  # burning coals
SCLERA = Material("sclera", (246, 246, 250), "plain")
EYE_IRIS = Material("eye_iris", (90, 170, 255), "plain", emissive=True)
EYE_IRIS_GOLD = Material("eye_iris_gold", (255, 200, 70), "plain", emissive=True)
PUPIL = Material("pupil", (16, 14, 24), "plain")
LID = Material("lid", (200, 190, 150), "skin")
LION = Material("lion", (214, 160, 72), "hair")
LION_MANE = Material("lion_mane", (170, 108, 40), "hair")
OX = Material("ox", (140, 96, 64), "hair")
HORN = Material("horn", (232, 222, 196), "plain")
EAGLE = Material("eagle", (110, 78, 50), "feather", accent=(70, 48, 30))
EAGLE_WHITE = Material("eagle_white", (240, 236, 226), "feather", accent=(200, 196, 186))
BEAK = Material("beak", (246, 196, 60), "plain")
BERYL_SHADE = Material("beryl_shade", (176, 190, 118), "crystal")
SERAPH_SKIN = Material("seraph_skin", (255, 226, 170), "skin")
SERAPH_SKIN_SHADE = Material("seraph_skin_shade", (240, 186, 120), "skin")
