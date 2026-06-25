import adbutils

class UniversalDeviceEngine:
    def __init__(self, device_serial: str, local_jar_path: str):
        self.device_serial = device_serial
        self.local_jar_path = local_jar_path
        self.remote_jar_path = "/data/local/tmp/universal_runner.jar"
        self._initialize_engine()

    def _initialize_engine(self):
        # Establish connection with the local ADB server
        adb = adbutils.AdbClient(host="127.0.0.1", port=5037)
        self.device = adb.device(serial=self.device_serial)
        
        # Simple caching: only push the JAR if it's missing on the device
        remote_files = [f.name for f in self.device.list_dir("/data/local/tmp")]
        if "universal_runner.jar" not in remote_files:
            self.device.sync.push(self.local_jar_path, self.remote_jar_path)
            self.device.shell(f"chmod 0644 {self.remote_jar_path}")
        
        # Boot up our persistent runner inside the phone's ART runtime
        cmd = f"CLASSPATH={self.remote_jar_path} app_process /data/local/tmp com.pentest.ide.UniversalRunner"
        self.connection = self.device.shell(cmd, stream=True)
        
        # Wait until the Java engine signals that it is ready to receive requests
        self.connection.read_until_string("READY\n")

    def send_command(self, action: str, target: str = "") -> list:
        """Sends an active request straight down to the running on-device Java loop."""
        command_str = f"{action}:{target}\n"
        self.connection.conn.sendall(command_str.encode('utf-8'))
        
        results = []
        # Keep reading incoming stream lines until the 'EOF' token is met
        while True:
            line = self.connection.conn.readline().decode('utf-8', errors='replace').strip()
            if line == "EOF":
                break
            if line.startswith("DATA\t"):
                results.append(line.replace("DATA\t", ""))
            elif line.startswith("ERROR\t"):
                raise Exception(line.replace("ERROR\t", ""))
        return results

    def close(self):
        """Cleanly tell the remote loop to spin down and close the pipe."""
        try:
            self.connection.conn.sendall(b"EXIT\n")
            self.connection.close()
        except:
            pass

if __name__ == "__main__":
    # Initialize the engine once
    engine = UniversalDeviceEngine("YOUR_DEVICE_SERIAL", "universal_runner.jar")
    
    print("Streaming application labels...")
    labels = engine.send_command("GET_LABELS")
    for entry in labels[:5]: 
        print(entry)
        
    engine.close()
