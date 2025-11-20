import json
from decimal import Decimal
from producto_controller_for_testing import (
    getFileExtension,
    parsear_productos_json,
    calcular_porcentaje_destacado
)

# -------------------------------
# TEST 1: getFileExtension
# -------------------------------
def test_getFileExtension():
    assert getFileExtension("imagen.jpg") == "jpg"
    assert getFileExtension("foto.PNG") == "png"
    assert getFileExtension("archivo.sin_extension") == "sin_extension"
    assert getFileExtension(None) == "jpg"


# -------------------------------
# TEST 2: parseo JSON múltiple
# -------------------------------
def test_parseo_json_multiple():
    json_prueba = """
    [
        {"nombre": "Prod1", "descripcion": "Desc1", "precio": 2000},
        {"nombre": "Prod2", "descripcion": "Desc2", "precio": 3500}
    ]
    """
    productos = parsear_productos_json(json_prueba)

    assert len(productos) == 2
    assert productos[0]["nombre"] == "Prod1"
    assert productos[1]["precio"] == 3500


# -------------------------------
# TEST 3: destacar porcentaje
# -------------------------------
def test_destacar_porcentaje_limites():
    # Porcentaje válido
    resultado = calcular_porcentaje_destacado("25")
    assert resultado == Decimal("25")

    # Vacío → 0
    assert calcular_porcentaje_destacado("") == Decimal("0")

    # Negativo → 0
    assert calcular_porcentaje_destacado("-10") == Decimal("0")

    # Mayor que 100 → 100
    assert calcular_porcentaje_destacado("150") == Decimal("100")

    # Invalido → 0
    assert calcular_porcentaje_destacado("abc") == Decimal("0")
