# NNPPSS

The **NNPPSS** application is a tool designed for the management and download of files related to PlayStation Vita. It is developed in Java and uses the Swing graphical library for the user interface.

### Main Features

1. **File Download**: The application allows the download of files from a remote URL in the background, avoiding user interface blocking during the process. Downloaded files can be both databases and game files.

2. **Filtering and Searching**: Users can filter and search for games in the loaded database. Filtering can be done by text and by region.

3. **Interaction with Data Table**: Users can select games from a data table to view detailed information about them and, in some cases, download the game directly from the application.

4. **Interaction with Downloaded Files**: After downloading a game, the application can execute commands in the background to perform actions such as decompressing files or other specific operations for the downloaded game.

### Advantages

- **User-Friendly Interface**: The application uses Swing to provide an intuitive and user-friendly interface, allowing users to navigate and operate the application with ease.

- **Background Download**: The background download feature allows users to perform other tasks while files are being downloaded, improving the user experience by avoiding interface locks.

- **Advanced Filtering**: The ability to filter and search for games in the database provides users with an efficient way to find the games they want, even in large databases.

- **Interaction with Downloaded Files**: The application offers additional functionalities, such as executing commands in the background to manipulate downloaded files, expanding the application's usability.

### Requirements

To be able to execute the **NNPPSS** application, the following elements are required:

1. **Java Runtime Environment (JRE)**: java 8 or higher version

2. **URL database**: obviously :p

3. **OS**: NNPPSS is compatible with Windows, macOS, and Linux or Unix Like.

4. [**pkg2zip**](https://github.com/lusid1/pkg2zip) installed on Linux
or MacOS (in the case of windows it is not required, it is already inside the zip file).

### Running the Application

To run the **NNPPSS** application from the JAR file, follow these steps:

1. Make sure you have Java Runtime Environment (JRE) installed on your system. You can download it from the official [Java website](https://www.oracle.com/java/technologies/javase-jre8-downloads.html).

2. Download .zip of the [**NNPPSS**](https://github.com/SquarePeace/NNPPSS/releases) application from the repository.

3. Open a terminal or command prompt.

4. Navigate to the directory where the JAR file is located.

5. Run the following command to execute the JAR file:

`java -jar NNPPSS.jar`

6. The application should start.

If you encounter any issues during the execution of the JAR file, make sure you have met all the requirements mentioned in the previous section and that the JAR file is located in the correct directory and if issue persists, you can open an [**issue**](https://github.com/SquarePeace/NNPPSS/issues).
