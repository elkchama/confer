import requests
import subprocess
import time

def test_usuario_controller():
    # Paso 1: Iniciar el servidor de Spring Boot
    proceso = subprocess.Popen(
        ["cmd", "/c", "mvn spring-boot:run"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )

    # Esperar unos segundos para que el servidor arranque
    time.sleep(20)

    try:
        # Paso 2: Probar endpoint de login (GET)
        respuesta_login = requests.get("http://localhost:8070/login")
        assert respuesta_login.status_code == 200, "El endpoint /login no respondió correctamente"

        # Paso 3: Probar endpoint de registro (GET)
        respuesta_registro = requests.get("http://localhost:8070/registro")
        assert respuesta_registro.status_code == 200, "El endpoint /registro no respondió correctamente"

        # Paso 4: Probar endpoint de bienvenida
        respuesta_bienvenida = requests.get("http://localhost:8070/bienvenida")
        assert respuesta_bienvenida.status_code == 200, "El endpoint /bienvenida no respondió correctamente"

        print("\n✅ Todos los endpoints básicos de UsuarioController respondieron correctamente.\n")

    finally:
        # Paso 5: Detener el servidor
        proceso.terminate()
        proceso.wait()
