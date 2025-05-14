===============================
Online Testing Portal - Setup Guide
===============================

📦 Overview
This portal allows teachers to create and assign multiple-choice tests, manage student class rosters, and view results.
Students can log in, take assigned tests, and see their own performance.

-------------------------------
📋 Requirements
-------------------------------
- MySQL Server and MySQL Workbench (installed via the MySQL Installer)
- A modern web browser (e.g., Chrome, Firefox)
- No Java installation needed — Java is already bundled in this package

-------------------------------
🛠️ Step 1: Install MySQL Server and MySQL Workbench
-------------------------------

To set up the database for this portal, you'll need two components:

- **MySQL Server** (the database engine)
- **MySQL Workbench** (the graphical interface to interact with your database)

If you don’t have either installed, follow these steps:

1. ✅ **Download MySQL Installer**
   - Visit: https://dev.mysql.com/downloads/installer/
   - Download the **"Windows (x86, 32-bit), MSI Installer" (full version)** — this includes both MySQL Server and MySQL Workbench

2. ✅ **Run the Installer and Add Required Products**
   - Launch the installer
   - On the "Select Products and Features" screen, you'll need to **manually add** MySQL Server and Workbench:

     a. Click **“Add”** to open the product catalog

     b. In the left sidebar, expand the tree:
        - `MySQL Servers > MySQL Server`
        - Then choose the **latest version (likely the top of the list) of MySQL Server 8.0** (e.g., 8.0.36) and click **Add**

     c. Next, expand:
        - `Applications > MySQL Workbench`
        - Select the **latest version** and click **Add**

     d. Click **Next** to continue with the install

3. ✅ **Configure MySQL Server**
   - When prompted for **Setup Type**, select:
     - **Developer Default** (recommended)
     - Or **Server Only** for a lighter install

   - Leave the port set to `3306`
   - Choose **Use Legacy Authentication Method**
   - Set a **root password** — write this down (you’ll use it later)
   - Finish the installation and configuration

4. ✅ **Start the MySQL Service**
   - Press `Windows + R`, type `services.msc`, and press Enter
   - Look for a service named `MySQL` or `MySQL80`
   - Ensure it's listed as **Running**
     - If it's not, right-click and choose **Start**

-------------------------------
🗃️ Step 2: Create the Database in MySQL Workbench
-------------------------------

1. Open **MySQL Workbench**
   - You should see a connection such as `Local instance MySQL80`
   - Click on it to open the SQL editor

   🧭 When it opens:
   - Two tabs will appear: an **Info tab** and a **Query tab** titled "Query 1"
   - Use the **Query 1 tab** (the one with a blinking cursor in the white editor window)

2. Create the database:
   - Type:
     CREATE DATABASE online_testing;
   - Click the **⚡ lightning bolt icon** to run it

3. Switch to the new database:
   - Delete the previous line from the editor
   - Type:
     USE online_testing;
   - Click the **⚡ lightning bolt icon** to run it

4. Import the schema:
   - Delete any old text in the editor first
   - Go to **File > Open SQL Script**
   - Select `schema.sql` from your `OnlineTestingPortal` folder
   - It will open in the editor
   - Click the **⚡ lightning bolt icon** to execute it

   💡 After running the script, if you don’t see the new tables:
   - Right-click the **Schemas** section on the left sidebar and choose **"Refresh All"**

5. Confirm your tables:
   - Expand the `online_testing` schema
   - You should see tables like `users`, `tests`, `questions`, `results`, etc.

6. You can now **close MySQL Workbench**

-------------------------------
🧩 Step 3: Configure App Database Credentials
-------------------------------

1. In the OnlineTestingPortal folder, navigate to:
   OnlineTestingPortal/WebContent/WEB-INF/classes/db.properties

2. Right-click `db.properties` and choose **Open with > Notepad**
- You may also use Notepad++, VS Code, or another text editor

3. Replace the contents so it looks like this:
   db.url=jdbc:mysql://localhost:3306/online_testing
   db.user=root
   db.password=your_mysql_password

🛑 IMPORTANT:  
Replace `your_mysql_password` with the **exact root password** you created when installing MySQL Server

4. Save and close the file

-------------------------------
🚀 Step 4: Launch the Portal
-------------------------------

1. In the OnlineTestingPortalPackage older, open the `OnlineTestingPortal` folder
2. Double-click `Deploy.bat`

This will:
- Deploy the app to a bundled Tomcat server
- Automatically open your default browser to:
http://localhost:8080/OnlineTestingPortal/

-------------------------------
👥 Default Login Info
-------------------------------

- **Teacher Login**
- Username: `teacher1`
- Password: `test123`

- Students are created by the teacher on the "Manage Class Roster" page.
Login credentials are auto-generated and shown after class creation.

-------------------------------
📁 Folder Summary
-------------------------------

- `apache-tomcat/` → Tomcat server (pre-configured)
- `jdk-17/` → Java runtime (no install required)
- `WebContent/` → HTML, .class files, and library dependencies
- `src/` → Java source code (optional for editing)
- `schema.sql` → MySQL schema file for initializing the database
- `Deploy.bat` → One-click launcher

-------------------------------
❓ Troubleshooting
-------------------------------

- If MySQL isn't running:
• Open `services.msc` and start the MySQL service

- If the browser doesn’t open:
• Open it manually and go to: http://localhost:8080/OnlineTestingPortal/

- If port 8080 is already in use:
• Edit `apache-tomcat/conf/server.xml` and change the connector port

- If you get a login error:
• Check that `db.properties` matches your actual MySQL username and password

