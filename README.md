## Prerequisites/Requirements

1. You will need Java, I recommend Java 17+
2. If you are building from source, maven is required.
3. For non-NT (windows) based systems, you will need pkg2zip 

## Installation

1. Clone the Repository `git clone https://github.com/SquarePeace/NNPPSS.git`
2. Compile with Maven `mvn compile`
3. Package with Mavem `mvn package`
Jar File should now be in `/target/NNPPSS-x.x.jar`
Run Jar File using `java -jar NPPSS-x.x.jar`

## Main Features

1. **File Download**: The application allows the download of files from a remote URL in the background, avoiding user interface blocking during the process. Downloaded files can be both databases and game files.

2. **Filtering and Searching**: Users can filter and search for games in the loaded database. Filtering can be done by text and by region.

3. **Interaction with Data Table**: Users can select games from a data table to view detailed information about them and, in some cases, download the game directly from the application.

4. **Interaction with Downloaded Files**: After downloading a game, the application can execute commands in the background to perform actions such as decompressing files or other specific operations for the downloaded game.

## Advantages

- **User-Friendly Interface**: The application uses Swing to provide an intuitive and user-friendly interface, allowing users to navigate and operate the application with ease.

- **Background Download**: The background download feature allows users to perform other tasks while files are being downloaded, improving the user experience by avoiding interface locks.

- **Advanced Filtering**: The ability to filter and search for games in the database provides users with an efficient way to find the games they want, even in large databases.

- **Interaction with Downloaded Files**: The application offers additional functionalities, such as executing commands in the background to manipulate downloaded files, expanding the application's usability.
