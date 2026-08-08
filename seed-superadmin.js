const { Client } = require('pg');
const bcrypt = require('bcryptjs');

async function seedSuperAdmin() {
  const client = new Client({
    connectionString: 'postgresql://postgres.kkweqesfxxnmdhsziycj:Edupaste7764@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?prepareThreshold=0',
    ssl: { rejectUnauthorized: false }
  });

  try {
    await client.connect();
    console.log('Connected to Supabase database');

    // Check if superadmin already exists
    const existing = await client.query(
      "SELECT id, email, role FROM users WHERE email = 'superadmin@edupaste.com'"
    );

    if (existing.rows.length > 0) {
      console.log('Super Admin already exists:', existing.rows[0]);
      console.log('Updating password to "password123"...');
      const hash = bcrypt.hashSync('password123', 10);
      await client.query(
        "UPDATE users SET password = $1 WHERE email = 'superadmin@edupaste.com'",
        [hash]
      );
      console.log('Password updated successfully.');
    } else {
      console.log('Creating new Super Admin...');
      const hash = bcrypt.hashSync('password123', 10);
      await client.query(
        `INSERT INTO users (full_name, email, password, role, school_id, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, NOW(), NOW())`,
        ['Super Admin', 'superadmin@edupaste.com', hash, 'SUPER_ADMIN', null]
      );
      console.log('Super Admin created successfully!');
    }

    // Verify
    const verify = await client.query(
      "SELECT id, full_name, email, role, school_id FROM users WHERE role = 'SUPER_ADMIN'"
    );
    console.log('\nAll SUPER_ADMIN users:', verify.rows);

  } catch (err) {
    console.error('Error:', err.message);
  } finally {
    await client.end();
  }
}

seedSuperAdmin();
