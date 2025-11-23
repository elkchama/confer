import subprocess
import requests
import time

def test_usuario_controller():
    # Paso 1: Iniciar el servidor de Spring Boot
    proceso = subprocess.Popen(
        ["cmd", "/c", "mvn spring-boot:run"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )

    print("Iniciando servidor Spring Boot...")
    time.sleep(25)  # esperar a que el servidor esté listo

    try:
        r_login = requests.get("http://localhost:8070/login")
        assert r_login.status_code == 200, " El endpoint /login no respondió correctamente"

        # Paso 3: Probar endpoint /registro
        r_registro = requests.get("http://localhost:8070/registro")
        assert r_registro.status_code == 200, " El endpoint /registro no respondió correctamente"

        # Paso 4: Probar endpoint /bienvenida
        r_bienvenida = requests.get("http://localhost:8070/bienvenida")
        assert r_bienvenida.status_code == 200, " El endpoint /bienvenida no respondió correctamente"

        print("\nTodos los endpoints básicos de UsuarioController respondieron correctamente.\n")

    except Exception as e:
        assert False, f" Error en las pruebas de UsuarioController: {e}"

    finally:
        # Paso 5: Detener el servidor
        print(" Deteniendo servidor...")

    # Esperar unos segundos para que el servidor arranque
    time.sleep(20)
