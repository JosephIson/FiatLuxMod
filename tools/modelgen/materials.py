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
# Ezekiel 1:13 "their appearance was like burning coals of fire": veins glow, the rest is coal
CHERUB_COAL = Material("cherub_coal", (92, 52, 40), "coal", emissive=True, glow_veins=True, accent=(255, 120, 30))
CHERUB_FLESH = Material("cherub_flesh", (206, 150, 110), "skin")
CHERUB_FLESH_SHADE = Material("cherub_flesh_shade", (176, 118, 86), "skin")
HOOF = Material("hoof", (210, 140, 60), "metal", emissive=True, accent=(255, 220, 150))  # burnished brass (1:7)
CHERUB_FEATHER = Material("cherub_feather", (232, 220, 196), "feather", accent=(170, 130, 70))
CHERUB_FEATHER_SHADE = Material("cherub_feather_shade", (204, 188, 160), "feather", accent=(150, 110, 60))
CHERUB_FEATHER_GOLD = Material("cherub_feather_gold", (226, 182, 90), "feather", accent=(160, 110, 40))
EYE_PAINTED = Material("eye_painted", (255, 186, 60), "eye", emissive=True)   # "full of eyes" (Ezek 10:12)
BEARD = Material("beard", (70, 46, 34), "hair")
LION_NOSE = Material("lion_nose", (70, 44, 40), "skin")
OX_NOSE = Material("ox_nose", (60, 46, 46), "skin")

# --- the Fallen (Isaiah 14:12, Luke 10:18, Revelation 12:9) --------------------------
# scorched where the lightning struck: ash-grey flesh cracked with smouldering crimson veins
ASH_SKIN = Material("ash_skin", (104, 96, 102), "skin")
ASH_SKIN_SHADE = Material("ash_skin_shade", (78, 70, 78), "skin")
SCORCHED = Material("scorched", (70, 60, 66), "coal", emissive=True, glow_veins=True, accent=(230, 40, 24))
RED_EYE = Material("red_eye", (255, 40, 24), "plain", emissive=True)
TATTER = Material("tatter", (44, 38, 46), "cloth", accent=(120, 26, 30))        # the linen, blackened
TATTER_SHADE = Material("tatter_shade", (32, 28, 34), "cloth", accent=(100, 20, 24))
TATTER_TRIM = Material("tatter_trim", (110, 30, 34), "cloth", accent=(60, 14, 16))  # the gold girdle, rusted crimson
BONE_STRUT = Material("bone_strut", (196, 182, 160), "plain")
BONE_DARK = Material("bone_dark", (140, 126, 110), "plain")
MEMBRANE = Material("membrane", (58, 34, 40), "membrane", alpha=236, accent=(170, 30, 30))
MEMBRANE_DARK = Material("membrane_dark", (40, 24, 30), "membrane", alpha=236, accent=(140, 24, 24))
BURNT_FEATHER = Material("burnt_feather", (70, 64, 66), "feather", accent=(40, 34, 36))
HALO_BROKEN = Material("halo_broken", (156, 122, 58), "gold", accent=(90, 64, 30))   # tarnished, no longer lit
HALO_CRACK = Material("halo_crack", (255, 80, 40), "fire", emissive=True, alpha=220, accent=(160, 20, 10))
HAIR_ASH = Material("hair_ash", (40, 36, 40), "hair")
