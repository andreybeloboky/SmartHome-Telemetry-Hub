SmartHomeHub is a console-based Java application that acts as a simple telemetry hub for receiving sensor data over a TCP connection and storing it in a PostgreSQL database. The program listens on port 8080, accepts incoming connections from sensor clients, reads a single line of text in the format DEVICE_NAME:VALUE, validates the input, and inserts the parsed data into the sensor_logs table using JDBC. The project is implemented in Java and uses environment variables for database configuration.

Getting started

You can run the Java program on your machine by opening a terminal.

**Step 1:** Install JDK 21 or newer.

**Step 2:** Open a terminal and navigate to the folder where the SmartHomeHub.java file is located.

**Step 3:** Compile the program using the javac command. For example: javac SmartHomeHub.java

**Step 4:** After compilation, run the program using the java command: java SmartHomeHub. The server will start and begin listening for incoming sensor connections on port 8080.

This project requires a PostgreSQL database with a table named sensor_logs. The table must be created before running the program. The table must contain the following fields: id (serial primary key), device_name (string), reading_value (double), recorded_at (timestamp with default current timestamp).

The application uses environment variables to read database credentials. The variables DB_LOGIN, DB_PASSWORD and DB_URL must be created before running the program.

On Windows PowerShell you can create them using the commands: setx DB_LOGIN "your_login", setx DB_PASSWORD "your_password", setx DB_URL "jdbc:postgresql://localhost:5432/your_database_name". After creating them, restart the terminal so the variables become available.

On Linux or macOS you can add them to your shell configuration file such as ~/.bashrc or ~/.zshrc by writing export DB_LOGIN="your_login", export DB_PASSWORD="your_password", export DB_URL="jdbc:postgresql://localhost:5432/your_database_name" and then applying the changes with the command source ~/.bashrc.

If you are running the program through IntelliJ IDEA, you can set these variables by opening Run → Edit Configurations and adding DB_LOGIN, DB_PASSWORD and DB_URL to the Environment variables field. The program will then be able to read them using System.getenv("DB_LOGIN"), System.getenv("DB_PASSWORD") and System.getenv("DB_URL").

The program workflow is as follows. The server starts and opens a ServerSocket on port 8080. A sensor connects to the hub using a TCP socket. The sensor sends a single line of text in the format DEVICE_NAME:VALUE. The hub validates the format and splits the string into two parts. The hub inserts the parsed data into the sensor_logs table. If the operation succeeds, the hub responds with STATUS:OK. The connection is then closed and the server waits for the next sensor.