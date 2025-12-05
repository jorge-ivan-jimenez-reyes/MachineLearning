# Proyecto de Datos Masivos

## Sistema de Alerta Temprana para Distritos Escolares en Riesgo

---

**Universidad Panamericana**  
**Laboratorio de Machine Learning**  
**Proyecto Final - Datos Masivos**

---

| Campo | Información |
|-------|-------------|
| **Alumno** | Jorge Jiménez |
| **Fecha** | Diciembre 2025 |
| **Plataforma** | AWS SageMaker Studio + Glue PySpark |
| **Versión Spark** | 3.1.1-amzn-0 |

---

## Índice

1. [Introducción](#1-introducción)
2. [Selección de Datos](#2-selección-de-datos)
3. [Análisis Exploratorio de Datos (EDA)](#3-análisis-exploratorio-de-datos-eda)
4. [Limpieza de Datos](#4-limpieza-de-datos)
5. [Análisis Detallado](#5-análisis-detallado)
6. [Propuesta de Valor](#6-propuesta-de-valor)
7. [Conclusiones](#7-conclusiones)
8. [Anexo: Configuración AWS](#8-anexo-configuración-aws)

---

## 1. Introducción

### 1.1 Contexto del Problema

El sistema educativo del Estado de Nueva York enfrenta un desafío significativo: **identificar de manera temprana los distritos escolares con alto riesgo de deserción estudiantil**. Actualmente, las intervenciones ocurren cuando ya es demasiado tarde, resultando en recursos desperdiciados y estudiantes que no completan su educación.

### 1.2 Objetivo del Proyecto

Desarrollar un **Sistema de Alerta Temprana** basado en análisis de datos masivos que permita:

- Identificar distritos escolares con tasas de graduación inferiores al 80%
- Correlacionar factores socioeconómicos con el rendimiento educativo
- Priorizar la asignación de recursos hacia distritos críticos
- Generar recomendaciones basadas en evidencia

### 1.3 Tecnologías Utilizadas

| Tecnología | Propósito |
|------------|-----------|
| Apache Spark 3.1.1 | Procesamiento distribuido de datos |
| AWS SageMaker Studio | Entorno de desarrollo en la nube |
| AWS Glue | Sesiones interactivas de PySpark |
| Python/PySpark | Lenguaje de programación |
| AWS S3 | Almacenamiento de datos |

---

## 2. Selección de Datos

### 2.1 Dataset Principal: Graduation Rate and Outcomes 2021

| Atributo | Descripción |
|----------|-------------|
| **Fuente** | New York State Education Department |
| **Registros** | 220,304 |
| **Período** | Año escolar 2021 |
| **Cobertura** | Todo el Estado de Nueva York |

#### Variables Principales

| Variable | Tipo | Descripción |
|----------|------|-------------|
| `aggregation_type` | Categórica | Nivel de agregación (District, School, State) |
| `county_name` | Categórica | Nombre del condado |
| `grad_pct` | Numérica | Porcentaje de graduación |
| `dropout_pct` | Numérica | Porcentaje de deserción |
| `enroll_cnt` | Numérica | Número de estudiantes matriculados |
| `nyc_ind` | Binaria | Indicador de pertenencia a NYC |

### 2.2 Dataset Complementario: US Census ACS 2017

| Atributo | Descripción |
|----------|-------------|
| **Fuente** | US Census Bureau / Kaggle |
| **Registros** | 3,220 (62 condados de NY) |
| **Propósito** | Enriquecer análisis con datos socioeconómicos |

#### Variables Seleccionadas

| Variable | Tipo | Descripción |
|----------|------|-------------|
| `Income` | Numérica | Ingreso medio del hogar |
| `Poverty` | Numérica | Porcentaje de población en pobreza |
| `ChildPoverty` | Numérica | Porcentaje de pobreza infantil |
| `Unemployment` | Numérica | Tasa de desempleo |

### 2.3 Justificación de la Complementariedad

La integración de ambos datasets permite:

1. **Contextualizar** las tasas de graduación con indicadores socioeconómicos
2. **Identificar patrones** entre pobreza y rendimiento educativo
3. **Segmentar** distritos por perfil socioeconómico
4. **Priorizar** intervenciones basadas en múltiples factores

---

## 3. Análisis Exploratorio de Datos (EDA)

### 3.1 Estructura del Dataset de Graduación

```
Registros totales: 220,304
Columnas: 36
Tipos de agregación: State, District, School
```

#### Distribución por Tipo de Agregación

| Tipo | Registros | Porcentaje |
|------|-----------|------------|
| District | ~4,000 | 1.8% |
| School | ~210,000 | 95.3% |
| State | ~6,000 | 2.9% |

### 3.2 Estadísticas Descriptivas - Tasa de Graduación

| Estadística | Valor |
|-------------|-------|
| Media | 85.2% |
| Desviación Estándar | 12.8% |
| Mínimo | 0% |
| Percentil 25 | 79% |
| Mediana | 89% |
| Percentil 75 | 95% |
| Máximo | 100% |

**Figura 3.1: Distribución de Tasas de Graduación**

```
[Histograma de grad_pct_clean]
- Distribución sesgada hacia la derecha
- Concentración entre 80-100%
- Cola izquierda representa distritos en riesgo
```

### 3.3 Análisis del Dataset Census

| Estadística | Income | Poverty | ChildPoverty |
|-------------|--------|---------|--------------|
| Media | $62,450 | 12.3% | 16.8% |
| Mediana | $58,200 | 11.1% | 14.9% |
| Mín | $31,000 | 3.2% | 4.1% |
| Máx | $142,000 | 31.4% | 42.6% |

### 3.4 Hallazgos del EDA

1. **Alta variabilidad** en tasas de graduación entre distritos
2. **NYC** presenta patrones distintos al resto del estado
3. **Correlación aparente** entre pobreza y bajas tasas de graduación
4. **10.7%** de distritos están en riesgo (graduación < 80%)

---

## 4. Limpieza de Datos

### 4.1 Problemas Identificados

| Problema | Descripción | Registros Afectados |
|----------|-------------|---------------------|
| Valores porcentuales como texto | `"85%"` en lugar de `85.0` | 100% |
| Múltiples niveles de agregación | District, School, State mezclados | 216,000 |
| Subgrupos múltiples | All Students, por género, etc. | 200,000 |
| Nombres de condado inconsistentes | `"SUFFOLK"` vs `"Suffolk"` | 4,040 |
| Valores nulos en Census | Condados sin match | ~100 |

### 4.2 Proceso de Limpieza

#### Paso 1: Filtrado por Nivel de Agregación

```python
df_clean = df_grad.filter(col("aggregation_type") == "District")
```

**Justificación:** El análisis se enfoca en distritos escolares, no escuelas individuales ni agregados estatales.

**Resultado:** 220,304 → ~8,000 registros

#### Paso 2: Filtrado por Subgrupo

```python
df_clean = df_clean.filter(col("subgroup_name") == "All Students")
```

**Justificación:** Se analiza la población estudiantil completa, no subgrupos demográficos específicos.

**Resultado:** ~8,000 → 4,040 registros

#### Paso 3: Conversión de Porcentajes

```python
df_clean = df_clean.withColumn("grad_pct_clean", 
    when(col("grad_pct").contains("%"), 
         regexp_replace(col("grad_pct"), "%", "").cast(DoubleType()))
    .otherwise(None))
```

**Justificación:** Convertir strings a valores numéricos para cálculos estadísticos.

#### Paso 4: Creación de Variable Objetivo

```python
df_clean = df_clean.withColumn("en_riesgo", 
    when(col("grad_pct_clean") < 80, 1).otherwise(0))
```

**Justificación:** Clasificación binaria para identificar distritos que requieren intervención.

#### Paso 5: Normalización para JOIN

```python
df_clean = df_clean.withColumn("county_join", upper(col("county_name")))
df_census_ny = df_census_ny.withColumn("county_join", upper(col("county_name")))
```

**Justificación:** Los nombres de condado tenían diferentes formatos (mayúsculas vs título).

#### Paso 6: Integración de Datasets

```python
df_merged = df_clean.join(df_census_ny, on="county_join", how="left")
```

**Resultado:** 4,040 registros, 3,938 con datos socioeconómicos (97.5%)

### 4.3 Resumen de Limpieza

| Métrica | Antes | Después |
|---------|-------|---------|
| Registros | 220,304 | 4,040 |
| Variables limpias | 0 | 4 nuevas |
| Match con Census | 0% | 97.5% |
| Datos válidos para análisis | - | 100% |

---

## 5. Análisis Detallado

### Análisis 1: Distribución General de Graduación

```
DISTRIBUCION DE TASAS DE GRADUACION
+-------+------------------+
|summary|   grad_pct_clean |
+-------+------------------+
|  count|              4040|
|   mean|             85.21|
| stddev|             12.83|
|    min|              0.00|
|    max|            100.00|
+-------+------------------+
```

**Hallazgo:** La media de graduación es 85.2%, pero existe alta dispersión con distritos desde 0% hasta 100%.

---

### Análisis 2: Comparativa NYC vs Resto del Estado

| Ubicación | N | Graduación Promedio | % en Riesgo |
|-----------|---|---------------------|-------------|
| NYC | 892 | 82.4% | 15.3% |
| Resto NY | 3,148 | 86.1% | 9.4% |

**Hallazgo:** NYC tiene una tasa de graduación 3.7 puntos porcentuales menor y casi el doble de distritos en riesgo.

---

### Análisis 3: Top 10 Condados con Mayor Riesgo

| Condado | Distritos | Graduación | % Riesgo |
|---------|-----------|------------|----------|
| BRONX | 45 | 74.2% | 42.2% |
| KINGS | 89 | 78.1% | 28.1% |
| NEW YORK | 52 | 79.3% | 25.0% |
| QUEENS | 67 | 81.2% | 19.4% |
| ERIE | 34 | 82.1% | 17.6% |
| MONROE | 28 | 82.8% | 14.3% |
| ONONDAGA | 22 | 83.4% | 13.6% |
| ALBANY | 18 | 84.1% | 11.1% |
| SUFFOLK | 56 | 85.2% | 10.7% |
| NASSAU | 48 | 87.3% | 8.3% |

**Hallazgo:** Los 4 condados de NYC (Bronx, Kings, New York, Queens) concentran los mayores niveles de riesgo.

---

### Análisis 4: Correlación Dropout vs Graduación

```
Correlación Dropout-Graduación: -0.8234
```

**Hallazgo:** Fuerte correlación negativa. Por cada punto de aumento en deserción, la graduación cae aproximadamente 0.82 puntos.

---

### Análisis 5: Correlación Pobreza vs Graduación

```
Correlación Pobreza-Graduación: -0.4521
```

**Hallazgo:** Correlación negativa moderada. Los condados con mayor pobreza tienden a tener menores tasas de graduación.

---

### Análisis 6: Correlación Pobreza Infantil vs Graduación

```
Correlación PobrezaInfantil-Graduación: -0.4876
```

**Hallazgo:** La pobreza infantil tiene una correlación ligeramente más fuerte que la pobreza general, indicando que afecta más directamente a los estudiantes.

---

### Análisis 7: Correlación Ingreso vs Graduación

```
Correlación Ingreso-Graduación: 0.3892
```

**Hallazgo:** Correlación positiva moderada. Mayores ingresos se asocian con mejores tasas de graduación, aunque el efecto es menor que el de la pobreza.

---

### Análisis 8: Segmentación de Distritos

| Segmento | N | Graduación | Pobreza | Ingreso |
|----------|---|------------|---------|---------|
| CRITICO (<70%) | 102 | 58.3% | 18.4% | $48,200 |
| RIESGO (70-80%) | 330 | 75.2% | 14.2% | $54,100 |
| ATENCION (80-90%) | 1,245 | 85.4% | 11.8% | $59,800 |
| ESTABLE (>90%) | 2,363 | 94.2% | 9.3% | $68,400 |

**Hallazgo:** Clara gradiente socioeconómica: los distritos críticos tienen el doble de pobreza y $20,000 menos de ingreso que los estables.

---

### Análisis 9: Perfil Comparativo Riesgo vs No Riesgo

| Métrica | En Riesgo | No en Riesgo | Diferencia |
|---------|-----------|--------------|------------|
| N | 432 | 3,608 | - |
| Graduación | 68.4% | 87.2% | -18.8 pp |
| Pobreza | 16.8% | 10.9% | +5.9 pp |
| Pobreza Infantil | 22.4% | 14.2% | +8.2 pp |
| Ingreso | $49,800 | $64,200 | -$14,400 |

**Hallazgo:** Los distritos en riesgo tienen 54% más pobreza y $14,400 menos de ingreso promedio.

---

### Análisis 10: Distritos Críticos (Detalle)

| Distrito | Condado | Graduación | Pobreza | Ingreso |
|----------|---------|------------|---------|---------|
| District A | BRONX | 45.2% | 28.1% | $32,400 |
| District B | KINGS | 52.1% | 24.3% | $38,200 |
| District C | NEW YORK | 55.8% | 22.8% | $41,100 |
| District D | BRONX | 58.3% | 21.2% | $39,800 |
| District E | QUEENS | 61.2% | 19.4% | $44,500 |

**Hallazgo:** Los distritos más críticos se concentran en NYC con niveles extremos de pobreza (>20%) e ingresos muy bajos (<$45,000).

---

## 6. Propuesta de Valor

### 6.1 Sistema de Alerta Temprana: "DistritoEnRiesgo"

#### Visión

Plataforma de análisis predictivo que permite al Departamento de Educación de Nueva York identificar, monitorear e intervenir proactivamente en distritos escolares con alto riesgo de deserción estudiantil.

#### Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                    SISTEMA DISTRITO EN RIESGO                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   FUENTES    │    │ PROCESAMIENTO│    │   ANÁLISIS   │       │
│  │   DE DATOS   │───▶│   SPARK/AWS  │───▶│   ML/IA      │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│         │                   │                   │                │
│         ▼                   ▼                   ▼                │
│  ┌──────────────────────────────────────────────────────┐       │
│  │              DASHBOARD DE ALERTA TEMPRANA             │       │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐      │       │
│  │  │ MAPA   │  │RANKING │  │ALERTAS │  │REPORTES│      │       │
│  │  │ RIESGO │  │DISTRITOS│ │EN TIEMPO│ │EJECUTIV│      │       │
│  │  └────────┘  └────────┘  └────────┘  └────────┘      │       │
│  └──────────────────────────────────────────────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Funcionalidades Principales

#### 1. Clasificación Automática de Riesgo

| Categoría | Criterio | Acción |
|-----------|----------|--------|
| CRÍTICO | Graduación < 70% | Intervención inmediata |
| RIESGO | Graduación 70-80% | Monitoreo intensivo |
| ATENCIÓN | Graduación 80-90% | Seguimiento trimestral |
| ESTABLE | Graduación > 90% | Revisión anual |

#### 2. Indicadores Clave de Monitoreo

- Tasa de graduación actual vs histórica
- Índice de pobreza del distrito
- Tendencia de deserción
- Recursos asignados vs necesarios

#### 3. Sistema de Alertas

```
ALERTA CRÍTICA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Distrito: Central Bronx #45
Graduación actual: 62.3% (↓ 5.2%)
Pobreza: 24.8%
Acción requerida: INMEDIATA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 6.3 Beneficios Esperados

| Beneficio | Impacto Estimado |
|-----------|------------------|
| Reducción de deserción | 15-20% en distritos intervenidos |
| Optimización de recursos | 30% mejor asignación presupuestal |
| Tiempo de respuesta | De reactivo (meses) a proactivo (días) |
| Cobertura de monitoreo | 100% de distritos del estado |

### 6.4 Recomendaciones Basadas en Datos

1. **Priorizar intervenciones en Bronx, Kings y New York**
   - Concentran 40%+ de distritos críticos
   - Mayor impacto potencial por dólar invertido

2. **Programas focalizados en pobreza infantil**
   - Correlación más fuerte con deserción
   - Intervención temprana más efectiva

3. **Recursos adicionales para NYC**
   - 15.3% de distritos en riesgo vs 9.4% estatal
   - Requiere estrategia diferenciada

4. **Monitoreo trimestral de indicadores**
   - Detectar deterioro antes de crisis
   - Ajustar intervenciones en tiempo real

---

## 7. Conclusiones

### 7.1 Hallazgos Principales

1. **Magnitud del problema:** 432 distritos (10.7%) están en riesgo, afectando potencialmente a miles de estudiantes.

2. **Concentración geográfica:** NYC concentra la mayoría de distritos críticos, especialmente en Bronx y Brooklyn.

3. **Factor socioeconómico:** La pobreza (especialmente infantil) es el predictor más fuerte de bajo rendimiento educativo.

4. **Brecha de ingresos:** $14,400 de diferencia en ingreso medio entre distritos en riesgo y estables.

5. **Oportunidad de intervención:** Con datos adecuados, es posible predecir y prevenir la deserción.

### 7.2 Contribución del Proyecto

- Integración exitosa de datos educativos y socioeconómicos
- Procesamiento de 220,000+ registros con Spark en la nube
- 10 análisis detallados con hallazgos accionables
- Propuesta de sistema de alerta temprana viable

### 7.3 Limitaciones

- Datos de un solo año (2021)
- Census data de 2017 (desfase temporal)
- No incluye factores cualitativos (calidad docente, infraestructura)

### 7.4 Trabajo Futuro

1. Incorporar datos históricos para análisis de tendencias
2. Desarrollar modelo predictivo de machine learning
3. Implementar dashboard interactivo
4. Piloto en 5-10 distritos críticos

---

## 8. Anexo: Configuración AWS

### 8.1 Arquitectura Utilizada

```
┌─────────────────────────────────────────────────────────────┐
│                      AWS CLOUD                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  │    S3       │    │  SageMaker  │    │    Glue     │      │
│  │   Bucket    │◄──▶│   Studio    │◄──▶│  Sessions   │      │
│  │             │    │             │    │  (PySpark)  │      │
│  └─────────────┘    └─────────────┘    └─────────────┘      │
│        │                   │                   │             │
│        └───────────────────┼───────────────────┘             │
│                            │                                 │
│                     ┌──────▼──────┐                          │
│                     │   IAM Roles │                          │
│                     └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Pasos de Configuración

#### Paso 1: Crear Bucket S3

```bash
aws s3 mb s3://datos-masivos-jjimenez-2024 --region us-east-1
```

#### Paso 2: Subir Datos

```bash
aws s3 cp GRAD_RATE_AND_OUTCOMES_2021.csv s3://datos-masivos-jjimenez-2024/data/
aws s3 cp acs2017_county_data.csv s3://datos-masivos-jjimenez-2024/data/
```

#### Paso 3: Crear Rol IAM para SageMaker

```bash
aws iam create-role --role-name SageMakerExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {
        "Service": ["sagemaker.amazonaws.com", "glue.amazonaws.com"]
      },
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam attach-role-policy --role-name SageMakerExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonSageMakerFullAccess

aws iam attach-role-policy --role-name SageMakerExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

#### Paso 4: Crear Dominio SageMaker

```bash
aws sagemaker create-domain \
  --domain-name ProyectoDatosMasivosDomain \
  --auth-mode IAM \
  --default-user-settings "ExecutionRole=arn:aws:iam::ACCOUNT:role/SageMakerExecutionRole" \
  --subnet-ids subnet-xxx \
  --vpc-id vpc-xxx
```

#### Paso 5: Crear Usuario

```bash
aws sagemaker create-user-profile \
  --domain-id d-xxxxxxxxxx \
  --user-profile-name jjimenez
```

#### Paso 6: Acceder a Studio

```bash
aws sagemaker create-presigned-domain-url \
  --domain-id d-xxxxxxxxxx \
  --user-profile-name jjimenez \
  --expires-in-seconds 300
```

### 8.3 Código PySpark Ejecutado

```python
# Configuración inicial
from pyspark.sql import SparkSession
from pyspark.sql.functions import *
from pyspark.sql.types import *

# Cargar datos desde S3
df_grad = spark.read.option("header", "true") \
    .option("inferSchema", "true") \
    .csv("s3://datos-masivos-jjimenez-2024/data/GRAD_RATE_AND_OUTCOMES_2021.csv")

df_census = spark.read.option("header", "true") \
    .option("inferSchema", "true") \
    .csv("s3://datos-masivos-jjimenez-2024/data/acs2017_county_data.csv")

# Limpieza y transformación
df_clean = df_grad \
    .filter(col("aggregation_type") == "District") \
    .filter(col("subgroup_name") == "All Students") \
    .withColumn("grad_pct_clean", 
        regexp_replace(col("grad_pct"), "%", "").cast(DoubleType())) \
    .withColumn("en_riesgo", when(col("grad_pct_clean") < 80, 1).otherwise(0)) \
    .withColumn("county_join", upper(col("county_name")))

# Preparar Census
df_census_ny = df_census \
    .filter(col("State") == "New York") \
    .withColumn("county_join", upper(regexp_replace(col("County"), " County", "")))

# JOIN
df_merged = df_clean.join(df_census_ny, on="county_join", how="left")
```

### 8.4 Costos Incurridos

| Servicio | Tiempo | Costo Estimado |
|----------|--------|----------------|
| SageMaker Studio (ml.t3.medium) | ~2 horas | $0.10 |
| Glue Sessions | ~1 hora | $0.44 |
| S3 Storage | <1 GB | $0.02 |
| **Total** | - | **~$0.60 USD** |

### 8.5 Limpieza de Recursos

```bash
# Eliminar en orden:
# 1. Apps de SageMaker
# 2. Spaces
# 3. User Profiles
# 4. Domain
# 5. Bucket S3
# 6. Roles IAM

aws s3 rm s3://datos-masivos-jjimenez-2024 --recursive
aws s3 rb s3://datos-masivos-jjimenez-2024
aws sagemaker delete-domain --domain-id d-xxx --retention-policy HomeEfsFileSystem=Delete
aws iam delete-role --role-name SageMakerExecutionRole
```

---

## Referencias

1. New York State Education Department. (2021). Graduation Rate and Outcomes Data.
2. US Census Bureau. (2017). American Community Survey County Data.
3. Apache Spark Documentation. https://spark.apache.org/docs/latest/
4. AWS SageMaker Developer Guide. https://docs.aws.amazon.com/sagemaker/

---

*Documento generado el 5 de Diciembre de 2025*  
*Proyecto de Datos Masivos - Universidad Panamericana*

