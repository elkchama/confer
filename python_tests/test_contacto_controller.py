import subprocess
import requests
import time

def test_contacto_controller():
    # Paso 1: Iniciar el servidor de Spring Boot
    proceso = subprocess.Popen(
        ["cmd", "/c", "mvn spring-boot:run"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )

    # Paso 2: Esperar unos segundos para que el servidor arranque
    print("Iniciando servidor Spring Boot...")
    time.sleep(25)  # puedes ajustar este tiempo si tu servidor tarda más o menos

    try:
        # Paso 3: Probar el endpoint de ContactoController
        respuesta = requests.get("http://localhost:8070/contactos")

        # Verificar si responde con éxito
        assert respuesta.status_code == 200, f" Error: código {respuesta.status_code}"
        print(" El endpoint /contactos respondió correctamente.")

    except Exception as e:
        assert False, f" Error al conectar con /contactos: {e}"

    finally:
        # Paso 4: Detener el servidor
        print(" Deteniendo servidor...")
        proceso.terminate()
        proceso.wait()
