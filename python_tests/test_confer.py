import subprocess
import requests

def test_contacto_controller():
    # Paso 1: Iniciar el servidor de Spring Boot (mvn spring-boot:run)
    proceso = subprocess.Popen(
        ["mvn", "spring-boot:run"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        shell=True
    )

    # Paso 2: Esperar unos segundos para que el servidor arranque
    import time
    time.sleep(30)

    # Paso 3: Probar el endpoint de ContactoController
    try:
        respuesta = requests.get("http://localhost:8070/contactos")

        # Verificar si responde con éxito
        assert respuesta.status_code == 200, f"Error: código {respuesta.status_code}"
        print("✅ El endpoint /contactos responde correctamente.")
    except Exception as e:
        assert False, f"❌ Error en la conexión: {e}"
    finally:
        # Paso 4: Detener el servidor de Spring Boot
        proceso.terminate()
