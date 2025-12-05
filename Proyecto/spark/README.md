# Proyecto Datos Masivos - Spark + Scala

## 📁 Estructura de Archivos

```
spark/
├── DataCleaning.scala      # Limpieza de datos con Spark
├── DataAnalysis.scala      # Análisis detallado (10 puntos)
├── README.md               # Este archivo
```

## 🚀 Cómo Ejecutar

### Opción 1: spark-shell (Local)

```bash
# Iniciar spark-shell y cargar el script
spark-shell

# Dentro de spark-shell:
:load spark/DataCleaning.scala
```

### Opción 2: spark-submit

```bash
# Compilar primero (si es necesario)
spark-submit --class DataCleaning spark/DataCleaning.scala
```

### Opción 3: Databricks / EMR Notebook

1. Crear un nuevo notebook en Databricks/EMR
2. Seleccionar lenguaje: Scala
3. Copiar el contenido de `DataCleaning.scala`
4. Ejecutar celda por celda

### Opción 4: AWS EMR

```bash
# Subir datos a S3
aws s3 cp data/ s3://tu-bucket/data/ --recursive

# Crear cluster EMR con Spark
aws emr create-cluster \
  --name "DistritoEnRiesgo" \
  --release-label emr-6.9.0 \
  --applications Name=Spark \
  --instance-type m5.xlarge \
  --instance-count 3
```

## 📊 Datasets

| Dataset | Archivo | Registros | Descripción |
|---------|---------|-----------|-------------|
| Principal | `GRAD_RATE_AND_OUTCOMES_2021.csv` | 220,304 | Tasas de graduación NY |
| Complementario | `acs2017_county_data.csv` | 3,220 | Datos socioeconómicos Census |

## 🧹 Proceso de Limpieza

1. **Carga de datos** - Leer CSVs con schema inference
2. **Filtrado** - Solo distritos y "All Students"
3. **Conversión de tipos** - Porcentajes string → Double
4. **Manejo de nulos** - Eliminar registros sin grad_pct
5. **Creación de target** - `en_riesgo = 1 si grad < 80%`
6. **Detección de outliers** - Método IQR
7. **JOIN de datasets** - Por county_name
8. **Guardado** - CSV y Parquet

## 📈 Output

```
data/cleaned/
├── graduation_census_merged/     # CSV
└── graduation_census_merged.parquet/  # Parquet (recomendado)
```

## 🔧 Requisitos

- Apache Spark 3.x
- Scala 2.12+
- Java 8 o 11

## 📝 Variables Finales

| Variable | Tipo | Descripción |
|----------|------|-------------|
| `lea_name` | String | Nombre del distrito |
| `county_name` | String | Nombre del condado |
| `grad_pct_clean` | Double | Tasa de graduación (%) |
| `en_riesgo` | Int | Target: 1=riesgo, 0=no riesgo |
| `dropout_pct_clean` | Double | Tasa de deserción (%) |
| `Income` | Double | Ingreso mediano (Census) |
| `Poverty` | Double | % en pobreza (Census) |
| `ChildPoverty` | Double | % pobreza infantil (Census) |
| `Unemployment` | Double | % desempleo (Census) |


