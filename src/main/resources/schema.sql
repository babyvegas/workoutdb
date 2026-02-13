CREATE TABLE IF NOT EXISTS exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    instructions TEXT NOT NULL,
    primary_muscle TEXT NOT NULL,
    secondary_muscle TEXT,
    equipment TEXT
);
