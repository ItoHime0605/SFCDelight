"""Scan *every* SFCDelight 1.21.1 recipe, including conditional and custom recipes.

RecipeManager previously rejected 61 vanilla-style recipes because the JSON
used raw strings instead of Minecraft 1.21.1 Ingredient objects. This checks
all 64 recipes so the same mistake cannot recur in newly added recipes.
"""
from pathlib import Path
import json
import re
ROOT=Path(__file__).resolve().parents[3]
BASE=ROOT/"src/main/resources"
REC=BASE/"data/sfcdelight/recipe"
SRC=ROOT/"src/main/java/tw/saltedfriedchicken"
recipes=sorted(REC.rglob("*.json"))
assert len(recipes)==64, ("unexpected recipe count; inspect new or removed recipes",len(recipes))
java=(SRC/"SaltedFriedChickenDelight.java").read_text(encoding="utf-8")
registered=set(re.findall(r'ITEMS\.register(?:SimpleItem|SimpleBlockItem)?\("([^"]+)"',java))
# The ITEM registry can also register items on a wrapped/newline declaration.
assert "chicken_cutting_knife" in registered
assert "pepper_shaker" in registered
changed=0
types={}
for file in recipes:
    name=file.relative_to(REC).as_posix()
    with file.open(encoding="utf-8") as stream:
        r=json.load(stream)
    kind=r.get("type")
    assert isinstance(kind,str),name
    types[kind]=types.get(kind,0)+1

    def ingredient(value,where):
        assert isinstance(value,dict),(name,where,"expected Ingredient object, not string/array")
        assert ("item" in value) != ("tag" in value),(name,where,"must contain exactly one item or tag")
        assert len(value)==1,(name,where,"unexpected ingredient fields",value)
        loc=value.get("item",value.get("tag"))
        assert isinstance(loc,str) and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+",loc), (name,where,loc)
        if loc.startswith("sfcdelight:"):
            key=loc.split(":",1)[1]
            if "item" in value:
                assert key in registered,(name,where,"unregistered local item",key)
            else:
                assert (BASE/"data/sfcdelight/tags/item"/(key+".json")).exists(),(name,where,"missing tag")
        return loc

    def output(value):
        assert isinstance(value,dict),(name,"recipe result should be an object")
        assert isinstance(value.get("id"),str),(name,"result id missing")
        assert re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_/.-]+",value["id"]),(name,value["id"])
        if value["id"].startswith("sfcdelight:"):
            assert value["id"].split(":",1)[1] in registered,(name,"unregistered output",value["id"])
        assert isinstance(value.get("count",1),int) and 1<=value.get("count",1)<=64,(name,"invalid result count")
        if "components" in value:
            assert isinstance(value["components"],dict),(name,"invalid item components")

    if kind=="minecraft:crafting_shaped":
        pattern=r.get("pattern")
        assert isinstance(pattern,list) and 1<=len(pattern)<=3,(name,"bad pattern")
        assert all(isinstance(row,str) and len(row)==len(pattern[0]) and 1<=len(row)<=3 for row in pattern),(name,"bad row")
        key=r.get("key")
        assert isinstance(key,dict),(name,"missing key")
        assert set("".join(pattern))-{" "}==set(key),(name,"unused/missing ingredient key")
        for k,v in key.items():
            assert len(k)==1 and k!=" ",(name,k)
            ingredient(v,"key."+k)
        output(r.get("result"))
    elif kind=="minecraft:crafting_shapeless":
        ing=r.get("ingredients")
        assert isinstance(ing,list) and 1<=len(ing)<=9,(name,"bad ingredient count")
        for i,v in enumerate(ing):ingredient(v,f"ingredients[{i}]")
        output(r.get("result"))
    elif kind=="minecraft:smelting":
        ingredient(r.get("ingredient"),"ingredient")
        output(r.get("result"))
        assert isinstance(r.get("cookingtime"),int) and r["cookingtime"]>0
    elif kind=="farmersdelight:cutting":
        # 3rd-party custom codec: preserve the previously loaded data shape.
        assert name=="cutting/chicken_with_special_knife.json",name
        for i,v in enumerate(r["ingredients"]):ingredient(v,f"ingredients[{i}]")
        ingredient(r["tool"],"tool")
        assert r["tool"]=={"item":"sfcdelight:chicken_cutting_knife"}
        result=r["result"]
        assert len(result)==2 and all(isinstance(v,dict) and "item" in v for v in result)
        for i,v in enumerate(result):output(v["item"])
    elif kind=="sfcdelight:pepper_refill":
        assert name=="pepper_refill.json"
        assert r.get("category")=="misc"
    else:
        raise AssertionError((name,"unexpected recipe serializer",kind))

    deps=r.get("neoforge:conditions",[])
    assert isinstance(deps,list)
    # Optional external ingredients require the provider mod, never an
    # unconditional reference to a missing item registration.
    required=set()
    def scan(value):
        if isinstance(value,dict):
            for k,v in value.items():
                if k in ("item","tag") and isinstance(v,str) and ":" in v:
                    namespace=v.split(":",1)[0]
                    if namespace not in ("sfcdelight","minecraft"):
                        required.add(namespace)
                else:scan(v)
        elif isinstance(value,list):
            for v in value:scan(v)
    if kind not in ("sfcdelight:pepper_refill",):
        scan(r.get("ingredients",r.get("ingredient",r.get("key",{}))))
    loaded={x.get("modid") for x in deps if isinstance(x,dict) and x.get("type")=="neoforge:mod_loaded"}
    # For the mod-specific cutting serializer, Farmer's Delight is also a dependency.
    required|=({"farmersdelight"} if kind=="farmersdelight:cutting" else set())
    assert required.issubset(loaded),(name,"missing optional mod conditions",sorted(required-loaded))
    changed+=1

serial=(SRC/"PepperRefillRecipe.java").read_text(encoding="utf-8")
assert not re.search(r"(?m)^\s*StreamCodec\.unit\s*\(", serial), "StreamCodec.unit checks object identity and disconnects clients"
assert "StreamCodec.of(" in serial
assert "new PepperRefillRecipe(CraftingBookCategory.MISC)" in serial
assert "return CODEC;" in serial and "return STREAM_CODEC;" in serial
assert types.get("minecraft:crafting_shaped",0)>15
assert types.get("minecraft:crafting_shapeless",0)>15
assert types.get("sfcdelight:pepper_refill")==1
assert types.get("farmersdelight:cutting")==1
print(f"PASS: {changed} recipe definitions, no raw-string ingredients, registered outputs, optional mod checks and identity-safe network serializer")
