from decimal import Decimal
import json

# -------------------------------
# getFileExtension (igual al de tu controlador)
# -------------------------------
def getFileExtension(filename):
    if filename is None or "." not in filename:
        return "jpg"
    return filename.split(".")[-1].lower()


# -------------------------------
# parseo del JSON de /guardar-multiple
# -------------------------------
def parsear_productos_json(texto_json):
    return json.loads(texto_json)


# -------------------------------
# lógica del porcentaje del controlador
# -------------------------------
def calcular_porcentaje_destacado(valor):
    if valor is None or valor.strip() == "":
        return Decimal("0")

    try:
        p = Decimal(valor.strip())
        if p < 0:
            return Decimal("0")
        if p > 100:
            return Decimal("100")
        return p
    except:
        return Decimal("0")
