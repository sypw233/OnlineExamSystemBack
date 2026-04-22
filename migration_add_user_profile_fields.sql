-- Migration: Add nickname and avatar columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);
