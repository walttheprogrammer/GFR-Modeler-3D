CREATE DATABASE IF NOT EXISTS bd_gfr_modeler 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE bd_gfr_modeler;

-- 1. Tabla de Usuarios
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    correo VARCHAR(100) NOT NULL UNIQUE,
    clave VARCHAR(255) NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de Materiales
CREATE TABLE materiales (
    id_material INT AUTO_INCREMENT PRIMARY KEY,
    nombre_material VARCHAR(80) NOT NULL UNIQUE,
    densidad_gcm3 DECIMAL(10,3) NOT NULL,
    precio_cm3 DECIMAL(10,2) NOT NULL
);

-- 3. Tabla de Proyectos
CREATE TABLE proyectos (
    id_proyecto INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_material INT NOT NULL,
    nombre_proyecto VARCHAR(100) NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_proyecto_usuario FOREIGN KEY (id_usuario) 
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_proyecto_material FOREIGN KEY (id_material) 
        REFERENCES materiales(id_material)
);

-- 4. Tabla de Parámetros y resultados
CREATE TABLE parametros (
    id_parametro INT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto INT NOT NULL UNIQUE,
    funcion_pared VARCHAR(150) NOT NULL,
    altura_cm DECIMAL(10,2) NOT NULL,
    espesor_cm DECIMAL(10,2) NOT NULL,
    volumen_objetivo_ml DECIMAL(10,2) NOT NULL,
    volumen_calculado_ml DECIMAL(10,2) DEFAULT 0.00,
    area_pared_cm2 DECIMAL(10,2) DEFAULT 0.00,
    volumen_material_cm3 DECIMAL(10,2) DEFAULT 0.00,
    costo_estimado DECIMAL(10,2) DEFAULT 0.00,
    CONSTRAINT fk_parametro_proyecto FOREIGN KEY (id_proyecto) 
        REFERENCES proyectos(id_proyecto) ON DELETE CASCADE
);

-- Usuarios de ejemplo
INSERT INTO usuarios (nombre_completo, usuario, correo, clave) VALUES 
('Walter Dario Maza Cuadrado', 'wdmazam', 'walter@ucv.edu.pe', '123456');

-- Materiales
INSERT INTO materiales (nombre_material, densidad_gcm3, precio_cm3) VALUES 
('Vidrio Templado', 2.500, 0.18),
('Resina PET', 1.380, 0.12),
('Polipropileno (PP)', 0.910, 0.09);