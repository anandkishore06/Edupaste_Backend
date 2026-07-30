require('dotenv').config();
const { Client } = require('pg');

async function testSupabase() {
    console.log("Testing Supabase connection...");
    
    // Construct the connection string from .env
    const url = process.env.SPRING_DATASOURCE_URL;
    // jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres
    // We need to extract host, port, db
    const cleanUrl = url.replace("jdbc:postgresql://", "");
    const [hostPort, dbWithParams] = cleanUrl.split("/");
    const db = dbWithParams.split("?")[0];
    const [host, port] = hostPort.split(":");
    
    const client = new Client({
        host: host,
        port: parseInt(port),
        database: db,
        user: process.env.SPRING_DATASOURCE_USERNAME,
        password: process.env.SPRING_DATASOURCE_PASSWORD,
        ssl: { rejectUnauthorized: false }
    });

    try {
        await client.connect();
        console.log("✅ Successfully connected to Supabase PostgreSQL!");
        
        // 1. Create the users table (simulating Hibernate DDL generation)
        console.log("Creating 'users' table if it doesn't exist...");
        await client.query(`
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                full_name VARCHAR(100) NOT NULL,
                email VARCHAR(150) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL,
                school_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        `);
        console.log("✅ Table 'users' is ready.");
        
        // 2. Insert a test user
        console.log("Inserting a test user (School Admin)...");
        const insertQuery = `
            INSERT INTO users (full_name, email, password, role) 
            VALUES ($1, $2, $3, $4)
            ON CONFLICT (email) DO NOTHING
            RETURNING id;
        `;
        const values = ['Aman Kumar', 'aman@edupaste.com', 'hashed_password_mock', 'SCHOOL_ADMIN'];
        
        const res = await client.query(insertQuery, values);
        if (res.rows.length > 0) {
            console.log(`✅ Success! Inserted user with ID: ${res.rows[0].id}`);
        } else {
            console.log("⚠️ User with email 'aman@edupaste.com' already exists in the table.");
        }
        
        // 3. Fetch all users
        console.log("Fetching users from table:");
        const users = await client.query("SELECT id, full_name, email, role, created_at FROM users");
        console.table(users.rows);

    } catch (err) {
        console.error("❌ Database connection or query failed:", err.message);
    } finally {
        await client.end();
    }
}

testSupabase();
