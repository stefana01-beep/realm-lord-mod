"""The historical figures a Summoning Token can call, written to
src/main/resources/data/realmlord/figures.json and their names and descriptions to the lang file.

Each: name, era, kind, rank (0 King, 1 Emperor, 2 Divine, 3 Saint), and what they were known for.
Stats (Command, Force, Intelligence, Politics) follow from rank and kind, nudged per figure."""
import json, os, re

# kind -> occupation, specialty, stat weights (C, F, I, P)
KINDS = {
    "general":    ("officer", None, (1.00, 0.85, 0.85, 0.55)),
    "warrior":    ("martial_artist", None, (0.80, 1.00, 0.60, 0.40)),
    "strategist": ("civil_servant", "capable_administrator", (0.75, 0.40, 1.00, 0.85)),
    "minister":   ("civil_servant", "capable_administrator", (0.50, 0.35, 0.90, 1.00)),
    "scholar":    ("scholar", None, (0.35, 0.30, 0.95, 0.80)),
    "doctor":     ("doctor", None, (0.30, 0.30, 1.00, 0.55)),
    "engineer":   ("mason", "master_builder", (0.45, 0.40, 1.00, 0.65)),
    "carpenter":  ("carpenter", "master_builder", (0.35, 0.50, 0.95, 0.45)),
    "smith":      ("blacksmith", "skilled_hands", (0.35, 0.60, 0.90, 0.40)),
    "weaver":     ("tailor", "skilled_hands", (0.30, 0.30, 0.90, 0.50)),
    "agronomist": ("farmer", "green_thumb", (0.35, 0.30, 0.95, 0.70)),
    "admiral":    ("officer", None, (0.95, 0.75, 0.90, 0.70)),
    "merchant":   ("grocer", "skilled_hands", (0.45, 0.35, 0.95, 0.90)),
}

# (name, era, kind, rank, known for)
FIGURES = [
    # ------------------------------------------------------------- Saint rank
    ("Sun Wu", "Spring and Autumn", "strategist", 3, "general of Wu, author of The Art of War"),
    ("Han Xin", "Western Han", "general", 3, "the general who won the Han its empire"),
    ("Bai Qi", "Warring States", "general", 3, "Qin's war god, victor at Changping"),
    ("Zhuge Liang", "Three Kingdoms", "strategist", 3, "chancellor of Shu Han, the Sleeping Dragon"),
    ("Guan Zhong", "Spring and Autumn", "minister", 3, "chancellor who made Qi the first hegemon"),
    ("Zhang Liang", "Western Han", "strategist", 3, "strategist who planned the Han's rise"),
    ("Xiao He", "Western Han", "minister", 3, "first chancellor of the Han"),
    ("Huo Qubing", "Western Han", "general", 3, "cavalry general who drove back the Xiongnu"),
    ("Wei Qing", "Western Han", "general", 3, "grand marshal of the wars against the Xiongnu"),
    ("Li Jing", "Tang", "general", 3, "Tang general who conquered the Eastern Turks"),
    ("Yue Fei", "Southern Song", "general", 3, "loyal general who fought the Jin"),
    ("Lu Ban", "Spring and Autumn", "carpenter", 3, "master craftsman, patron of builders"),
    ("Hua Tuo", "Eastern Han", "doctor", 3, "physician famed for surgery and anaesthesia"),
    ("Jiang Ziya", "Western Zhou", "strategist", 3, "strategist who helped Zhou overthrow Shang"),
    # ------------------------------------------------------------- Divine rank
    ("Wang Jian", "Warring States", "general", 2, "Qin general who conquered Zhao and Chu"),
    ("Li Mu", "Warring States", "general", 2, "Zhao general who held the north against the Xiongnu"),
    ("Lian Po", "Warring States", "general", 2, "veteran general of Zhao"),
    ("Wu Qi", "Warring States", "general", 2, "general and reformer of Wei and Chu"),
    ("Sun Bin", "Warring States", "strategist", 2, "Qi strategist of Guiling and Maling"),
    ("Yue Yi", "Warring States", "general", 2, "Yan general who took seventy cities of Qi"),
    ("Tian Dan", "Warring States", "general", 2, "Qi general of the fire-ox charge"),
    ("Shang Yang", "Warring States", "minister", 2, "reformer who made Qin strong"),
    ("Li Si", "Qin", "minister", 2, "chancellor who unified Qin's laws and script"),
    ("Fan Li", "Spring and Autumn", "merchant", 2, "minister of Yue who became a great merchant"),
    ("Wu Zixu", "Spring and Autumn", "general", 2, "general and minister of Wu"),
    ("Ban Chao", "Eastern Han", "general", 2, "general who secured the Western Regions"),
    ("Zhou Yu", "Three Kingdoms", "general", 2, "Wu commander at Red Cliffs"),
    ("Sima Yi", "Three Kingdoms", "strategist", 2, "Wei strategist and regent"),
    ("Guan Yu", "Three Kingdoms", "warrior", 2, "Shu general famed for loyalty"),
    ("Zhao Yun", "Three Kingdoms", "warrior", 2, "Shu general famed for courage at Changban"),
    ("Zhang Liao", "Three Kingdoms", "general", 2, "Wei general of Hefei"),
    ("Lu Xun", "Three Kingdoms", "general", 2, "Wu commander at Yiling"),
    ("Deng Ai", "Three Kingdoms", "general", 2, "Wei general who took Shu"),
    ("Xie Xuan", "Eastern Jin", "general", 2, "general who won at the Fei River"),
    ("Wang Meng", "Former Qin", "minister", 2, "chancellor of Former Qin"),
    ("Fang Xuanling", "Tang", "minister", 2, "chancellor of Emperor Taizong"),
    ("Wei Zheng", "Tang", "minister", 2, "outspoken minister of Emperor Taizong"),
    ("Guo Ziyi", "Tang", "general", 2, "general who ended the An Lushan rebellion"),
    ("Di Renjie", "Tang", "minister", 2, "chancellor and famed judge"),
    ("Xu Da", "Ming", "general", 2, "general who drove the Yuan north"),
    ("Liu Ji", "Ming", "strategist", 2, "strategist of the Ming founding"),
    ("Qi Jiguang", "Ming", "general", 2, "general who defeated the coastal pirates"),
    ("Yu Qian", "Ming", "minister", 2, "minister who defended Beijing"),
    ("Zhang Juzheng", "Ming", "minister", 2, "senior grand secretary and reformer"),
    ("Zhang Zhongjing", "Eastern Han", "doctor", 2, "physician of the Treatise on Cold Damage"),
    ("Sun Simiao", "Tang", "doctor", 2, "physician, the King of Medicine"),
    ("Bian Que", "Warring States", "doctor", 2, "legendary physician"),
    ("Mozi", "Warring States", "engineer", 2, "philosopher and master of defensive works"),
    ("Zhang Heng", "Eastern Han", "engineer", 2, "astronomer and inventor of the seismoscope"),
    ("Zheng He", "Ming", "admiral", 2, "admiral of the great treasure fleets"),
    ("Su Dingfang", "Tang", "general", 2, "Tang general of the western campaigns"),
    ("Xue Rengui", "Tang", "general", 2, "Tang general famed in the eastern campaigns"),
    ("Li Bing", "Warring States", "engineer", 2, "builder of the Dujiangyan irrigation system"),
    # ------------------------------------------------------------- Emperor rank
    ("Lu Bu", "Eastern Han", "warrior", 1, "the mightiest warrior of his age"),
    ("Zhang Fei", "Three Kingdoms", "warrior", 1, "Shu general of Changban Bridge"),
    ("Ma Chao", "Three Kingdoms", "warrior", 1, "Shu general from Xiliang"),
    ("Huang Zhong", "Three Kingdoms", "warrior", 1, "veteran Shu general of Mount Dingjun"),
    ("Dian Wei", "Three Kingdoms", "warrior", 1, "Cao Cao's bodyguard"),
    ("Xu Chu", "Three Kingdoms", "warrior", 1, "Cao Cao's bodyguard, the Tiger Fool"),
    ("Gan Ning", "Three Kingdoms", "general", 1, "Wu general famed for daring raids"),
    ("Taishi Ci", "Three Kingdoms", "warrior", 1, "Wu general and archer"),
    ("Xun Yu", "Three Kingdoms", "strategist", 1, "Cao Cao's chief advisor"),
    ("Guo Jia", "Three Kingdoms", "strategist", 1, "Cao Cao's brilliant advisor"),
    ("Lu Su", "Three Kingdoms", "strategist", 1, "Wu strategist of the alliance with Shu"),
    ("Pang Tong", "Three Kingdoms", "strategist", 1, "Shu strategist, the Young Phoenix"),
    ("Jia Xu", "Three Kingdoms", "strategist", 1, "Wei strategist"),
    ("Xu Shu", "Three Kingdoms", "strategist", 1, "strategist who served Liu Bei"),
    ("Sima Qian", "Western Han", "scholar", 1, "historian of the Records of the Grand Historian"),
    ("Cai Lun", "Eastern Han", "engineer", 1, "official credited with improving papermaking"),
    ("Meng Tian", "Qin", "general", 1, "Qin general who built the northern walls"),
    ("Zhang Han", "Qin", "general", 1, "last great general of Qin"),
    ("Xiang Yu", "Qin", "warrior", 1, "the Hegemon-King of Western Chu"),
    ("Ying Bu", "Western Han", "general", 1, "general of Chu and Han"),
    ("Peng Yue", "Western Han", "general", 1, "Han general of raids behind Chu lines"),
    ("Cao Shen", "Western Han", "minister", 1, "second chancellor of the Han"),
    ("Chen Ping", "Western Han", "strategist", 1, "Han strategist and chancellor"),
    ("Zhou Yafu", "Western Han", "general", 1, "general who put down the Seven States"),
    ("Li Guang", "Western Han", "general", 1, "the Flying General, a great archer"),
    ("Ma Yuan", "Eastern Han", "general", 1, "the Wave-Calming General"),
    ("Yang Hu", "Western Jin", "general", 1, "Jin general who planned the conquest of Wu"),
    ("Du Yu", "Western Jin", "general", 1, "Jin general and scholar"),
    ("Xie An", "Eastern Jin", "minister", 1, "chancellor at the Fei River"),
    ("Zu Chongzhi", "Southern Dynasties", "scholar", 1, "mathematician who calculated pi"),
    ("Jia Sixie", "Northern Wei", "agronomist", 1, "author of the Essential Techniques for the Common People"),
    ("Qin Qiong", "Tang", "warrior", 1, "Tang general, later a door god"),
    ("Yuchi Gong", "Tang", "warrior", 1, "Tang general, later a door god"),
    ("Li Shiji", "Tang", "general", 1, "Tang general of many campaigns"),
    ("Zhangsun Wuji", "Tang", "minister", 1, "chancellor of Emperor Taizong"),
    ("Du Ruhui", "Tang", "minister", 1, "chancellor of Emperor Taizong"),
    ("Yao Chong", "Tang", "minister", 1, "chancellor of the Kaiyuan era"),
    ("Song Jing", "Tang", "minister", 1, "chancellor of the Kaiyuan era"),
    ("Han Shizhong", "Southern Song", "general", 1, "Song general at Huangtiandang"),
    ("Fan Zhongyan", "Northern Song", "minister", 1, "statesman of the Qingli reforms"),
    ("Wang Anshi", "Northern Song", "minister", 1, "chancellor of the New Policies"),
    ("Bao Zheng", "Northern Song", "minister", 1, "upright judge, Lord Bao"),
    ("Sima Guang", "Northern Song", "scholar", 1, "historian of the Comprehensive Mirror"),
    ("Shen Kuo", "Northern Song", "engineer", 1, "polymath of the Dream Pool Essays"),
    ("Bi Sheng", "Northern Song", "carpenter", 1, "inventor of movable type"),
    ("Su Song", "Northern Song", "engineer", 1, "builder of the astronomical clock tower"),
    ("Kou Zhun", "Northern Song", "minister", 1, "chancellor at Chanyuan"),
    ("Yang Ye", "Northern Song", "general", 1, "general of the Yang family"),
    ("Di Qing", "Northern Song", "general", 1, "Song general of the southwest"),
    ("Chang Yuchun", "Ming", "general", 1, "Ming founding general"),
    ("Li Shanchang", "Ming", "minister", 1, "first chancellor of the Ming"),
    ("Wang Shouren", "Ming", "scholar", 1, "philosopher and general, Wang Yangming"),
    ("Hai Rui", "Ming", "minister", 1, "incorruptible official"),
    ("Yuan Chonghuan", "Ming", "general", 1, "defender of Ningyuan"),
    ("Zheng Chenggong", "Ming", "admiral", 1, "admiral who took Taiwan, Koxinga"),
    ("Song Yingxing", "Ming", "engineer", 1, "author of The Exploitation of the Works of Nature"),
    ("Xu Guangqi", "Ming", "agronomist", 1, "scholar of agriculture and mathematics"),
    ("Li Shizhen", "Ming", "doctor", 1, "author of the Compendium of Materia Medica"),
    ("Huang Daopo", "Yuan", "weaver", 1, "pioneer of cotton spinning and weaving"),
    ("Guo Shoujing", "Yuan", "engineer", 1, "astronomer and hydraulic engineer"),
    ("Ge Hong", "Eastern Jin", "doctor", 1, "physician and alchemist"),
    ("Ma Jun", "Three Kingdoms", "engineer", 1, "inventor of the south-pointing chariot"),
    ("Ou Yezi", "Spring and Autumn", "smith", 1, "legendary swordsmith"),
    ("Gan Jiang", "Spring and Autumn", "smith", 1, "legendary swordsmith of Wu"),
    # ------------------------------------------------------------- King rank
    ("Lin Xiangru", "Warring States", "minister", 0, "Zhao minister who returned the jade intact"),
    ("Yan Ying", "Spring and Autumn", "minister", 0, "chancellor of Qi"),
    ("Zichan", "Spring and Autumn", "minister", 0, "statesman of Zheng"),
    ("Baili Xi", "Spring and Autumn", "minister", 0, "minister of Qin, bought for five ram skins"),
    ("Zhang Yi", "Warring States", "strategist", 0, "diplomat of the horizontal alliance"),
    ("Su Qin", "Warring States", "strategist", 0, "diplomat of the vertical alliance"),
    ("Fan Ju", "Warring States", "minister", 0, "Qin chancellor"),
    ("Lu Buwei", "Warring States", "merchant", 0, "merchant who became Qin's chancellor"),
    ("Pang Juan", "Warring States", "general", 0, "general of Wei"),
    ("Zhao She", "Warring States", "general", 0, "Zhao general of Eyu"),
    ("Sima Rangju", "Spring and Autumn", "general", 0, "Qi general and military writer"),
    ("Wen Zhong", "Spring and Autumn", "minister", 0, "minister of Yue"),
    ("Fan Zeng", "Qin", "strategist", 0, "advisor of Xiang Yu"),
    ("Ji Bu", "Western Han", "general", 0, "general famed for keeping his word"),
    ("Long Ju", "Qin", "general", 0, "Chu general under Xiang Yu"),
    ("Zhongli Mo", "Qin", "general", 0, "Chu general under Xiang Yu"),
    ("Fan Kuai", "Western Han", "warrior", 0, "Han warrior of the Hongmen feast"),
    ("Xiahou Ying", "Western Han", "general", 0, "Han general and charioteer"),
    ("Guan Ying", "Western Han", "general", 0, "Han cavalry general"),
    ("Huo Guang", "Western Han", "minister", 0, "regent of the Han"),
    ("Dong Zhongshu", "Western Han", "scholar", 0, "Confucian scholar of the Han"),
    ("Ban Gu", "Eastern Han", "scholar", 0, "historian of the Book of Han"),
    ("Huang Gai", "Three Kingdoms", "general", 0, "Wu general of the fire attack"),
    ("Lu Meng", "Three Kingdoms", "general", 0, "Wu general who took Jing Province"),
    ("Xiahou Dun", "Three Kingdoms", "general", 0, "Wei general"),
    ("Xiahou Yuan", "Three Kingdoms", "general", 0, "Wei general famed for swift marches"),
    ("Cao Ren", "Three Kingdoms", "general", 0, "Wei general, defender of Fancheng"),
    ("Wei Yan", "Three Kingdoms", "general", 0, "Shu general of Hanzhong"),
    ("Jiang Wei", "Three Kingdoms", "general", 0, "Shu general, Zhuge Liang's successor"),
    ("Fa Zheng", "Three Kingdoms", "strategist", 0, "Shu strategist"),
    ("Jiang Wan", "Three Kingdoms", "minister", 0, "Shu chancellor"),
    ("Fei Yi", "Three Kingdoms", "minister", 0, "Shu chancellor"),
    ("Cheng Yaojin", "Tang", "warrior", 0, "Tang general"),
    ("Zhang Xun", "Tang", "general", 0, "defender of Suiyang"),
    ("Li Guangbi", "Tang", "general", 0, "Tang general of the An Lushan rebellion"),
    ("Han Yu", "Tang", "scholar", 0, "essayist of the classical prose movement"),
    ("Ouyang Xiu", "Northern Song", "scholar", 0, "statesman and essayist"),
    ("Su Shi", "Northern Song", "scholar", 0, "poet, essayist and official"),
    ("Li Bai", "Tang", "scholar", 0, "the Immortal Poet"),
    ("Du Fu", "Tang", "scholar", 0, "the Poet Sage"),
    ("Yan Zhenqing", "Tang", "scholar", 0, "calligrapher and loyal official"),
    ("Yang Yanzhao", "Northern Song", "general", 0, "general of the Yang family"),
    ("Zong Ze", "Southern Song", "general", 0, "defender of Kaifeng"),
    ("Xin Qiji", "Southern Song", "general", 0, "poet and soldier"),
    ("Lan Yu", "Ming", "general", 0, "Ming general of the northern campaigns"),
    ("Mu Ying", "Ming", "general", 0, "Ming general who held Yunnan"),
    ("Yu Dayou", "Ming", "general", 0, "general against the coastal pirates"),
    ("Lu Xiangsheng", "Ming", "general", 0, "late Ming general"),
    ("Shi Kefa", "Ming", "minister", 0, "defender of Yangzhou"),
    ("Wang Zhen", "Yuan", "agronomist", 0, "author of the Treatise on Agriculture"),
    ("Zhang Qian", "Western Han", "strategist", 0, "envoy who opened the Silk Road"),
    ("Zhao Chongguo", "Western Han", "general", 0, "Han general of the western frontier"),
    ("Deng Yu", "Eastern Han", "general", 0, "general of the Han restoration"),
    ("Wang Xizhi", "Eastern Jin", "scholar", 0, "the Sage of Calligraphy"),
]

BASE = {0: (62, 18), 1: (72, 16), 2: (82, 13), 3: (90, 10)}   # rank -> (low, span) for the main stat


def slug(name):
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def stats(name, kind, rank):
    lo, span = BASE[rank]
    h = sum(ord(c) * (i + 7) for i, c in enumerate(name))
    out = []
    for k, w in enumerate(KINDS[kind][2]):
        jitter = ((h >> (k * 3)) % (span + 1))
        out.append(max(10, min(100, round((lo + jitter) * w + (1 - w) * 25))))
    return out


def build():
    seen = set()
    figures = []
    for name, era, kind, rank, note in FIGURES:
        fid = slug(name)
        assert fid not in seen, fid
        seen.add(fid)
        occ, spec, _ = KINDS[kind]
        c, f, i, p = stats(name, kind, rank)
        figures.append({"id": fid, "name": name, "era": era, "kind": kind, "rank": rank, "occupation": occ,
                        "specialty": spec or "", "command": c, "force": f, "intelligence": i, "politics": p})
    return figures


def lang(L):
    for name, era, kind, rank, note in FIGURES:
        fid = slug(name)
        L[f"realmlord.figure.{fid}"] = name
        L[f"realmlord.figure.{fid}.desc"] = f"{era}: {note}."
    for k in KINDS:
        L[f"realmlord.figure.kind.{k}"] = k.capitalize()


if __name__ == "__main__":
    out = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "realmlord", "figures.json")
    figs = build()
    with open(out, "w") as fh:
        json.dump(figs, fh, indent=1)
    from collections import Counter
    print(len(figs), "figures", Counter(f["rank"] for f in figs))
