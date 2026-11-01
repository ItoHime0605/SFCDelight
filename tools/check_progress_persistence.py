from pathlib import Path
s = (Path(__file__).resolve().parents[1] / 'src/main/java/tw/saltedfriedchicken/FryingBasketBlockEntity.java').read_text()
assert 'private static final int COOK_TICKS = 300' in s
assert 'basket.progress++;\n        basket.setChanged();' in s
assert 'basket.progress = 0;\n            basket.setChanged();\n            fryer.setFrying(false);' in s
print('油炸計時存檔靜態檢查通過')
