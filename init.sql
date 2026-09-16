-- =====================================================================
-- FarmaYopin Database Initialization Script (MySQL 8.0)
-- Configurado para docker-compose (montado en /docker-entrypoint-initdb.d)
-- =====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS `farmayopin_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `farmayopin_db`;

SET NAMES utf8mb4;

-- Desactivar temporalmente foreign keys para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `items_compra`;
DROP TABLE IF EXISTS `items_carrito`;
DROP TABLE IF EXISTS `compras`;
DROP TABLE IF EXISTS `carritos`;
DROP TABLE IF EXISTS `productos`;
DROP TABLE IF EXISTS `usuarios`;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. TABLA: usuarios
-- ---------------------------------------------------------------------
CREATE TABLE `usuarios` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `rol` ENUM('ADMIN','CLIENTE') NOT NULL DEFAULT 'CLIENTE',
  `fecha_creacion` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuarios_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 2. TABLA: carritos
-- ---------------------------------------------------------------------
CREATE TABLE `carritos` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `usuario_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_carritos_usuario` (`usuario_id`),
  CONSTRAINT `fk_carritos_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 3. TABLA: productos
-- ---------------------------------------------------------------------
CREATE TABLE `productos` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(150) NOT NULL,
  `precio` DECIMAL(10,2) NOT NULL,
  `detalle` TEXT,
  `foto` VARCHAR(500) DEFAULT NULL,
  `stock` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 4. TABLA: items_carrito
-- ---------------------------------------------------------------------
CREATE TABLE `items_carrito` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `carrito_id` BIGINT NOT NULL,
  `producto_id` BIGINT NOT NULL,
  `cantidad` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `fk_items_carrito_carrito_idx` (`carrito_id`),
  KEY `fk_items_carrito_producto_idx` (`producto_id`),
  CONSTRAINT `fk_items_carrito_carrito` FOREIGN KEY (`carrito_id`) REFERENCES `carritos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_items_carrito_producto` FOREIGN KEY (`producto_id`) REFERENCES `productos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 5. TABLA: compras
-- ---------------------------------------------------------------------
CREATE TABLE `compras` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `usuario_id` BIGINT NOT NULL,
  `fecha` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `total` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_compras_usuario_idx` (`usuario_id`),
  CONSTRAINT `fk_compras_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 6. TABLA: items_compra
-- ---------------------------------------------------------------------
CREATE TABLE `items_compra` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `compra_id` BIGINT NOT NULL,
  `producto_id` BIGINT DEFAULT NULL,
  `nombre_producto` VARCHAR(150) NOT NULL,
  `cantidad` INT NOT NULL,
  `precio_unitario` DECIMAL(10,2) NOT NULL,
  `subtotal` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_items_compra_compra_idx` (`compra_id`),
  KEY `fk_items_compra_producto_idx` (`producto_id`),
  CONSTRAINT `fk_items_compra_compra` FOREIGN KEY (`compra_id`) REFERENCES `compras` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_items_compra_producto` FOREIGN KEY (`producto_id`) REFERENCES `productos` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================================
-- DATOS DE PRUEBA (SEED DATA)
-- =====================================================================

-- ---------------------------------------------------------------------
-- SEED: Usuarios (Admin y Clientes)
-- Passwords BCrypt:
-- 'admin123'    -> $2a$10$KTKM/GnrKrCTCoxo.CIdTeQojAwhC6CGgA8D/mW.VeyW36c.aXPR6
-- 'password123' -> $2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.
-- ---------------------------------------------------------------------
INSERT INTO `usuarios` (`id`, `nombre`, `email`, `password`, `rol`, `fecha_creacion`) VALUES
(1, 'Administrador FarmaYopin', 'admin@farmayopin.com', '$2a$10$KTKM/GnrKrCTCoxo.CIdTeQojAwhC6CGgA8D/mW.VeyW36c.aXPR6', 'ADMIN', NOW()),
(2, 'Gastón Pérez', 'gaston@farmayopin.com', '$2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.', 'CLIENTE', NOW()),
(3, 'Gastón Pérez', 'gastonaso16@gmail.com', '$2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.', 'CLIENTE', NOW()),
(4, 'María González', 'maria.gonzalez@farmayopin.com', '$2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.', 'CLIENTE', NOW()),
(5, 'Carlos Rodríguez', 'carlos.rodriguez@farmayopin.com', '$2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.', 'CLIENTE', NOW()),
(6, 'Lucía Martínez', 'lucia.martinez@farmayopin.com', '$2a$10$6AxXLMPSiS8dEvrGSpmt0Ot9Uoaatmj9HlWOTcbD12103PLf9DGq.', 'CLIENTE', NOW());

-- ---------------------------------------------------------------------
-- SEED: Carritos para los clientes
-- ---------------------------------------------------------------------
INSERT INTO `carritos` (`id`, `usuario_id`) VALUES
(1, 2),
(2, 3),
(3, 4),
(4, 5),
(5, 6);

-- ---------------------------------------------------------------------
-- SEED: Catálogo de Productos con Imágenes Genéricas
-- ---------------------------------------------------------------------
INSERT INTO `productos` (`id`, `nombre`, `precio`, `detalle`, `foto`, `stock`) VALUES
(1, 'Paracetamol 500 mg', 1500.00, 'Caja de 20 comprimidos. Indicado para el alivio sintomático de dolores ocasionales leves o moderados y estados febriles.', 'img/paracetamol.png', 50),
(2, 'Ibuprofeno 400 mg', 2200.00, 'Caja de 20 cápsulas blandas de rápida absorción. Antiinflamatorio no esteroideo, analgésico y antipirético.', 'img/ibuprofeno.png', 40),
(3, 'Amoxicilina 500 mg', 3800.00, 'Caja de 16 comprimidos recubiertos. Antibiótico bactericida de amplio espectro para infecciones respiratorias y dentales.', 'img/amoxicilina.png', 25),
(4, 'Omeprazol 20 mg', 2900.00, 'Caja de 30 cápsulas gastrorresistentes. Inhibidor de la bomba de protones para el reflujo ácido y ardor estomacal.', 'img/omeprazol.png', 35),
(5, 'Loratadina 10 mg', 1850.00, 'Caja de 10 comprimidos. Antihistamínico para el alivio de rinitis alérgica, urticaria y picazón.', 'img/loratadina.png', 30),
(6, 'Alcohol en Gel 70% 500ml', 1950.00, 'Frasco dosificador sanitizante de manos con agentes humectantes y glicerina. Elimina el 99.9% de bacterias.', 'img/alcohol_gel.png', 60),
(7, 'Termómetro Digital Clínico', 4500.00, 'Termómetro con punta flexible y pantalla LCD. Medición precisa en 10 segundos con alarma sonora de fiebre.', 'img/termometro.png', 15),
(8, 'Protector Solar FPS 50+ 200ml', 8900.00, 'Protección solar muy alta contra rayos UVA y UVB. Fórmula hipoalergénica, resistente al agua y no grasa.', 'img/protector_solar.png', 20),
(9, 'Complejo Vitamínico B + C', 3400.00, 'Tubo de 60 comprimidos efervescentes sabor naranja. Aporta energía, vitalidad y fortalece el sistema inmune.', 'img/vitaminas.png', 45),
(10, 'Gasas Estériles y Venda Elástica', 1200.00, 'Set de primeros auxilios: 10 sobres de gasa hidrófila estéril más venda elástica de 5cm x 3m.', 'img/primeros_auxilios.png', 80),
(11, 'Jarabe para la Tos Infantil 120ml', 3100.00, 'Jarabe expectorante y fluidificante para niños con delicioso sabor a frutilla. Alivia la tos seca y productiva.', 'img/jarabe_tos.png', 22),
(12, 'Colirio Gotas Oftálmicas 15ml', 2650.00, 'Gotas lubricantes y descongestivas para ojos cansados, secos o irritados por pantallas y polvillo.', 'img/colirio.png', 18);

-- ---------------------------------------------------------------------
-- SEED: Compras de prueba con Historial
-- ---------------------------------------------------------------------
INSERT INTO `compras` (`id`, `usuario_id`, `fecha`, `total`) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), 5200.00),
(2, 2, DATE_SUB(NOW(), INTERVAL 5 HOUR), 10850.00),
(3, 4, DATE_SUB(NOW(), INTERVAL 1 DAY), 3400.00);

-- ---------------------------------------------------------------------
-- SEED: Ítems de las compras de prueba
-- ---------------------------------------------------------------------
INSERT INTO `items_compra` (`id`, `compra_id`, `producto_id`, `nombre_producto`, `cantidad`, `precio_unitario`, `subtotal`) VALUES
(1, 1, 1, 'Paracetamol 500 mg', 2, 1500.00, 3000.00),
(2, 1, 2, 'Ibuprofeno 400 mg', 1, 2200.00, 2200.00),
(3, 2, 8, 'Protector Solar FPS 50+ 200ml', 1, 8900.00, 8900.00),
(4, 2, 6, 'Alcohol en Gel 70% 500ml', 1, 1950.00, 1950.00),
(5, 3, 9, 'Complejo Vitamínico B + C', 1, 3400.00, 3400.00);
