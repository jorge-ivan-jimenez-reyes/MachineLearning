/**
 * ============================================================
 * PROYECTO: Predicción de Distritos Escolares en Riesgo
 * CURSO: Datos Masivos
 * ============================================================
 * 
 * Este script realiza la limpieza y preparación de datos usando
 * Apache Spark con Scala para el análisis de tasas de graduación
 * en distritos escolares de Nueva York.
 * 
 * Datasets:
 * - Principal: GRAD_RATE_AND_OUTCOMES_2021.csv (Educación NY)
 * - Complementario: acs2017_county_data.csv (Census Bureau)
 */

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

// ============================================================
// 1. INICIALIZACIÓN DE SPARK SESSION
// ============================================================

val spark = SparkSession.builder()
  .appName("DistritoEnRiesgo_DataCleaning")
  .master("local[*]")  // Cambiar a "yarn" para cluster
  .config("spark.sql.legacy.timeParserPolicy", "LEGACY")
  .getOrCreate()

import spark.implicits._

println("=" * 60)
println("LIMPIEZA DE DATOS - PROYECTO DATOS MASIVOS")
println("=" * 60)

// ============================================================
// 2. CARGA DE DATOS
// ============================================================

println("\n📁 Cargando datasets...")

// Dataset Principal: Tasas de Graduación
val dfGradRate = spark.read
  .option("header", "true")
  .option("inferSchema", "true")
  .option("quote", "\"")
  .option("escape", "\"")
  .csv("data/GRAD_RATE_AND_OUTCOMES_2021.csv")

// Dataset Complementario: Datos Socioeconómicos del Census
val dfCensus = spark.read
  .option("header", "true")
  .option("inferSchema", "true")
  .csv("data/acs2017_county_data.csv")

println(s"✅ Dataset Graduación: ${dfGradRate.count()} registros, ${dfGradRate.columns.length} columnas")
println(s"✅ Dataset Census: ${dfCensus.count()} registros, ${dfCensus.columns.length} columnas")

// ============================================================
// 3. EXPLORACIÓN INICIAL
// ============================================================

println("\n📊 Schema Dataset Graduación:")
dfGradRate.printSchema()

println("\n📊 Schema Dataset Census:")
dfCensus.printSchema()

println("\n📋 Muestra de datos - Graduación:")
dfGradRate.show(5, truncate = false)

println("\n📋 Muestra de datos - Census:")
dfCensus.show(5, truncate = false)

// ============================================================
// 4. ANÁLISIS DE VALORES NULOS
// ============================================================

println("\n🔍 ANÁLISIS DE VALORES NULOS - Dataset Graduación")
println("-" * 50)

// Función para contar nulos por columna
def countNulls(df: org.apache.spark.sql.DataFrame): Unit = {
  val totalRows = df.count()
  df.columns.foreach { col =>
    val nullCount = df.filter(df(col).isNull || df(col) === "" || df(col) === "s").count()
    val nullPct = (nullCount.toDouble / totalRows * 100)
    if (nullPct > 0) {
      println(f"  $col%-30s: $nullCount%8d nulls ($nullPct%.1f%%)")
    }
  }
}

countNulls(dfGradRate)

println("\n🔍 ANÁLISIS DE VALORES NULOS - Dataset Census")
println("-" * 50)
countNulls(dfCensus)

// ============================================================
// 5. LIMPIEZA DATASET GRADUACIÓN
// ============================================================

println("\n🧹 LIMPIEZA DE DATOS - Dataset Graduación")
println("-" * 50)

// 5.1 Convertir porcentajes de String a Double
// Los campos de porcentaje vienen como "85.5%" o "s" (suppressed)
val dfGradCleaned = dfGradRate
  // Filtrar solo distritos y "All Students"
  .filter(col("aggregation_type") === "District")
  .filter(col("subgroup_name") === "All Students")
  // Convertir porcentajes a numérico
  .withColumn("grad_pct_clean", 
    when(col("grad_pct").contains("%"), 
      regexp_replace(col("grad_pct"), "%", "").cast(DoubleType))
    .otherwise(null))
  .withColumn("dropout_pct_clean",
    when(col("dropout_pct").contains("%"),
      regexp_replace(col("dropout_pct"), "%", "").cast(DoubleType))
    .otherwise(null))
  .withColumn("local_pct_clean",
    when(col("local_pct").contains("%"),
      regexp_replace(col("local_pct"), "%", "").cast(DoubleType))
    .otherwise(null))
  .withColumn("reg_adv_pct_clean",
    when(col("reg_adv_pct").contains("%"),
      regexp_replace(col("reg_adv_pct"), "%", "").cast(DoubleType))
    .otherwise(null))
  .withColumn("still_enr_pct_clean",
    when(col("still_enr_pct").contains("%"),
      regexp_replace(col("still_enr_pct"), "%", "").cast(DoubleType))
    .otherwise(null))
  // Eliminar registros sin tasa de graduación
  .filter(col("grad_pct_clean").isNotNull)
  // Crear variable target: en_riesgo = 1 si graduación < 80%
  .withColumn("en_riesgo", when(col("grad_pct_clean") < 80, 1).otherwise(0))

println(s"✅ Registros después de limpieza: ${dfGradCleaned.count()}")
println(s"✅ Distritos únicos: ${dfGradCleaned.select("lea_name").distinct().count()}")

// 5.2 Verificar duplicados
val duplicados = dfGradCleaned
  .groupBy("lea_name", "county_name")
  .count()
  .filter(col("count") > 1)
  .count()

println(s"⚠️  Registros duplicados por distrito+condado: $duplicados")

// 5.3 Estadísticas del target
println("\n📊 Distribución del Target:")
dfGradCleaned.groupBy("en_riesgo")
  .agg(
    count("*").alias("cantidad"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio")
  )
  .orderBy("en_riesgo")
  .show()

// ============================================================
// 6. LIMPIEZA DATASET CENSUS
// ============================================================

println("\n🧹 LIMPIEZA DE DATOS - Dataset Census")
println("-" * 50)

// 6.1 Filtrar solo New York
val dfCensusNY = dfCensus
  .filter(col("State") === "New York")
  // Limpiar nombre del condado para hacer JOIN
  // Census tiene: "Albany County" -> necesitamos: "Albany"
  .withColumn("county_name_clean", 
    regexp_replace(col("County"), " County", ""))
  // Seleccionar columnas relevantes
  .select(
    col("county_name_clean").alias("county_name"),
    col("TotalPop"),
    col("Income"),
    col("IncomePerCap"),
    col("Poverty"),
    col("ChildPoverty"),
    col("Unemployment"),
    col("Professional"),
    col("Service"),
    col("Drive"),
    col("Transit"),
    col("White"),
    col("Black"),
    col("Hispanic"),
    col("Asian")
  )

println(s"✅ Condados de NY en Census: ${dfCensusNY.count()}")

// 6.2 Verificar valores nulos en Census NY
println("\n🔍 Valores nulos en Census NY:")
countNulls(dfCensusNY)

// 6.3 Estadísticas descriptivas
println("\n📊 Estadísticas Census NY:")
dfCensusNY.describe("Income", "Poverty", "ChildPoverty", "Unemployment").show()

// ============================================================
// 7. DETECCIÓN DE OUTLIERS (IQR)
// ============================================================

println("\n🔍 DETECCIÓN DE OUTLIERS - Tasa de Graduación")
println("-" * 50)

// Calcular IQR para grad_pct_clean
val quantiles = dfGradCleaned
  .stat
  .approxQuantile("grad_pct_clean", Array(0.25, 0.5, 0.75), 0.01)

val q1 = quantiles(0)
val median = quantiles(1)
val q3 = quantiles(2)
val iqr = q3 - q1
val lowerBound = q1 - 1.5 * iqr
val upperBound = q3 + 1.5 * iqr

println(f"  Q1 (25%%): $q1%.2f%%")
println(f"  Mediana:  $median%.2f%%")
println(f"  Q3 (75%%): $q3%.2f%%")
println(f"  IQR:      $iqr%.2f")
println(f"  Límite inferior: $lowerBound%.2f%%")
println(f"  Límite superior: $upperBound%.2f%%")

val outliers = dfGradCleaned
  .filter(col("grad_pct_clean") < lowerBound || col("grad_pct_clean") > upperBound)
  .count()

println(f"\n⚠️  Outliers detectados: $outliers (${outliers.toDouble / dfGradCleaned.count() * 100}%.1f%%)")

// Marcar outliers (no eliminar, son importantes para el análisis)
val dfGradWithOutliers = dfGradCleaned
  .withColumn("is_outlier", 
    when(col("grad_pct_clean") < lowerBound || col("grad_pct_clean") > upperBound, 1)
    .otherwise(0))

// ============================================================
// 8. JOIN DE DATASETS
// ============================================================

println("\n🔗 INTEGRACIÓN DE DATASETS")
println("-" * 50)

val dfMerged = dfGradWithOutliers
  .join(dfCensusNY, Seq("county_name"), "left")

println(s"✅ Registros después del JOIN: ${dfMerged.count()}")

// Verificar éxito del JOIN
val registrosConCensus = dfMerged.filter(col("Income").isNotNull).count()
val registrosSinCensus = dfMerged.filter(col("Income").isNull).count()

println(s"✅ Registros con datos socioeconómicos: $registrosConCensus")
println(s"⚠️  Registros sin match en Census: $registrosSinCensus")

// Condados sin match
if (registrosSinCensus > 0) {
  println("\n📋 Condados sin datos socioeconómicos:")
  dfMerged
    .filter(col("Income").isNull)
    .select("county_name")
    .distinct()
    .show(10)
}

// ============================================================
// 9. SELECCIÓN DE FEATURES FINALES
// ============================================================

println("\n📋 DATASET FINAL")
println("-" * 50)

val dfFinal = dfMerged.select(
  // Identificadores
  col("lea_name"),
  col("county_name"),
  col("nyc_ind"),
  
  // Variables del target
  col("grad_pct_clean"),
  col("en_riesgo"),
  col("is_outlier"),
  
  // Features educativas
  col("dropout_pct_clean"),
  col("still_enr_pct_clean"),
  col("local_pct_clean"),
  col("reg_adv_pct_clean"),
  col("enroll_cnt"),
  
  // Features socioeconómicas (Census)
  col("TotalPop"),
  col("Income"),
  col("IncomePerCap"),
  col("Poverty"),
  col("ChildPoverty"),
  col("Unemployment"),
  col("Professional")
)

println(s"✅ Dataset final: ${dfFinal.count()} registros, ${dfFinal.columns.length} columnas")
println("\n📊 Schema final:")
dfFinal.printSchema()

// ============================================================
// 10. GUARDAR DATOS LIMPIOS
// ============================================================

println("\n💾 GUARDANDO DATOS LIMPIOS")
println("-" * 50)

// Guardar como CSV
dfFinal
  .coalesce(1)
  .write
  .mode("overwrite")
  .option("header", "true")
  .csv("data/cleaned/graduation_census_merged")

// Guardar como Parquet (mejor para Spark)
dfFinal
  .write
  .mode("overwrite")
  .parquet("data/cleaned/graduation_census_merged.parquet")

println("✅ Datos guardados en data/cleaned/")

// ============================================================
// 11. RESUMEN DE LIMPIEZA
// ============================================================

println("\n" + "=" * 60)
println("📋 RESUMEN DEL PROCESO DE LIMPIEZA")
println("=" * 60)

println(s"""
|  Métrica                              |  Valor
|---------------------------------------|------------------
|  Registros originales (Graduación)    |  ${dfGradRate.count()}
|  Registros después de filtrar         |  ${dfGradCleaned.count()}
|  Distritos únicos                     |  ${dfGradCleaned.select("lea_name").distinct().count()}
|  Condados únicos                      |  ${dfGradCleaned.select("county_name").distinct().count()}
|  Distritos en riesgo (<80%)           |  ${dfGradCleaned.filter(col("en_riesgo") === 1).count()}
|  Outliers detectados                  |  $outliers
|  Registros con datos Census           |  $registrosConCensus
|  Registros finales                    |  ${dfFinal.count()}
""")

println("\n✅ Limpieza completada exitosamente!")

// ============================================================
// 12. CIERRE
// ============================================================

// Descomentar para cerrar la sesión
// spark.stop()

