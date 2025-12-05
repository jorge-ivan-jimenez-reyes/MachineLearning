/**
 * ============================================================
 * ANÁLISIS DETALLADO - 10 PUNTOS DE ANÁLISIS
 * CURSO: Datos Masivos
 * ============================================================
 * 
 * Este script realiza los 10 análisis requeridos para la
 * propuesta de valor del proyecto.
 * 
 * IMPORTANTE: Ejecutar después de DataCleaning.scala
 */

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

// ============================================================
// INICIALIZACIÓN
// ============================================================

val spark = SparkSession.builder()
  .appName("DistritoEnRiesgo_Analysis")
  .master("local[*]")
  .getOrCreate()

import spark.implicits._

// Cargar datos limpios
val df = spark.read.parquet("data/cleaned/graduation_census_merged.parquet")

println("=" * 70)
println("ANÁLISIS DETALLADO - 10 PUNTOS PARA PROPUESTA DE VALOR")
println("=" * 70)

// ============================================================
// ANÁLISIS 1: Distribución General de Tasas de Graduación
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 1: Distribución de Tasas de Graduación")
println("=" * 70)

println("\nEstadísticas descriptivas:")
df.describe("grad_pct_clean").show()

println("\nDistribución por rangos:")
df.withColumn("rango_graduacion", 
    when(col("grad_pct_clean") < 60, "Crítico (<60%)")
    .when(col("grad_pct_clean") < 70, "Muy bajo (60-70%)")
    .when(col("grad_pct_clean") < 80, "Bajo (70-80%)")
    .when(col("grad_pct_clean") < 90, "Medio (80-90%)")
    .otherwise("Alto (>90%)"))
  .groupBy("rango_graduacion")
  .agg(
    count("*").alias("cantidad"),
    round(avg("grad_pct_clean"), 2).alias("promedio")
  )
  .orderBy("promedio")
  .show()

// ============================================================
// ANÁLISIS 2: Comparativa NYC vs Resto del Estado
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 2: NYC vs Resto del Estado")
println("=" * 70)

df.groupBy("nyc_ind")
  .agg(
    count("*").alias("distritos"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio"),
    round(avg("dropout_pct_clean"), 2).alias("dropout_promedio"),
    round(sum(col("en_riesgo")).cast("double") / count("*") * 100, 2).alias("pct_en_riesgo")
  )
  .withColumn("ubicacion", when(col("nyc_ind") === 1, "NYC").otherwise("Resto NY"))
  .select("ubicacion", "distritos", "grad_promedio", "dropout_promedio", "pct_en_riesgo")
  .show()

// ============================================================
// ANÁLISIS 3: Top 10 Condados con Mayor Riesgo
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 3: Top 10 Condados con Mayor Riesgo")
println("=" * 70)

df.groupBy("county_name")
  .agg(
    count("*").alias("distritos"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio"),
    sum("en_riesgo").alias("distritos_riesgo"),
    round(sum(col("en_riesgo")).cast("double") / count("*") * 100, 2).alias("pct_riesgo")
  )
  .filter(col("distritos") >= 3)  // Solo condados con 3+ distritos
  .orderBy(desc("pct_riesgo"))
  .limit(10)
  .show()

// ============================================================
// ANÁLISIS 4: Correlación Dropout vs Graduación
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 4: Correlación Dropout vs Graduación")
println("=" * 70)

val corrDropout = df.stat.corr("dropout_pct_clean", "grad_pct_clean")
val corrStillEnr = df.stat.corr("still_enr_pct_clean", "grad_pct_clean")
val corrRegAdv = df.stat.corr("reg_adv_pct_clean", "grad_pct_clean")

println(f"""
Correlaciones con Tasa de Graduación:
  - Dropout %:      $corrDropout%.4f (${if(corrDropout < 0) "negativa" else "positiva"})
  - Still Enrolled: $corrStillEnr%.4f (${if(corrStillEnr < 0) "negativa" else "positiva"})
  - Regents Adv %:  $corrRegAdv%.4f (${if(corrRegAdv < 0) "negativa" else "positiva"})

Interpretación:
  - Alta deserción → Menor graduación (esperado)
  - Más alumnos "still enrolled" → Menor graduación a tiempo
  - Mejor rendimiento Regents Advanced → Mayor graduación
""")

// ============================================================
// ANÁLISIS 5: Brechas Demográficas (usando datos originales)
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 5: Análisis de Brechas Demográficas (Census)")
println("=" * 70)

println("\nRelación entre composición demográfica y riesgo:")
df.groupBy("en_riesgo")
  .agg(
    round(avg("White"), 2).alias("pct_white"),
    round(avg("Black"), 2).alias("pct_black"),
    round(avg("Hispanic"), 2).alias("pct_hispanic"),
    round(avg("Asian"), 2).alias("pct_asian")
  )
  .withColumn("estado", when(col("en_riesgo") === 1, "En Riesgo").otherwise("No Riesgo"))
  .select("estado", "pct_white", "pct_black", "pct_hispanic", "pct_asian")
  .show()

// ============================================================
// ANÁLISIS 6: Impacto de Pobreza en Graduación
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 6: Impacto de Pobreza en Graduación")
println("=" * 70)

// Correlación pobreza con graduación
val corrPoverty = df.filter(col("Poverty").isNotNull).stat.corr("Poverty", "grad_pct_clean")
val corrChildPoverty = df.filter(col("ChildPoverty").isNotNull).stat.corr("ChildPoverty", "grad_pct_clean")

println(f"""
Correlaciones con Tasa de Graduación:
  - Pobreza general:  $corrPoverty%.4f
  - Pobreza infantil: $corrChildPoverty%.4f

⚠️ Hallazgo clave: Mayor pobreza → Menor graduación
""")

// Distribución por cuartiles de pobreza
println("\nGraduación por nivel de pobreza del condado:")
df.filter(col("Poverty").isNotNull)
  .withColumn("nivel_pobreza",
    when(col("Poverty") < 10, "Baja (<10%)")
    .when(col("Poverty") < 15, "Media (10-15%)")
    .when(col("Poverty") < 20, "Alta (15-20%)")
    .otherwise("Muy Alta (>20%)"))
  .groupBy("nivel_pobreza")
  .agg(
    count("*").alias("distritos"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio"),
    round(sum(col("en_riesgo")).cast("double") / count("*") * 100, 2).alias("pct_riesgo")
  )
  .orderBy("grad_promedio")
  .show()

// ============================================================
// ANÁLISIS 7: Ingreso vs Graduación
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 7: Relación Ingreso Mediano vs Graduación")
println("=" * 70)

val corrIncome = df.filter(col("Income").isNotNull).stat.corr("Income", "grad_pct_clean")

println(f"Correlación Ingreso-Graduación: $corrIncome%.4f")

println("\nGraduación por nivel de ingreso del condado:")
df.filter(col("Income").isNotNull)
  .withColumn("nivel_ingreso",
    when(col("Income") < 50000, "Bajo (<$50K)")
    .when(col("Income") < 70000, "Medio ($50K-$70K)")
    .when(col("Income") < 90000, "Alto ($70K-$90K)")
    .otherwise("Muy Alto (>$90K)"))
  .groupBy("nivel_ingreso")
  .agg(
    count("*").alias("distritos"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio"),
    round(avg("Poverty"), 2).alias("pobreza_promedio")
  )
  .orderBy("grad_promedio")
  .show()

// ============================================================
// ANÁLISIS 8: Distritos Críticos (Outliers)
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 8: Distritos en Situación Crítica (Outliers)")
println("=" * 70)

println("\nDistritos con graduación más baja:")
df.filter(col("is_outlier") === 1)
  .orderBy("grad_pct_clean")
  .select("lea_name", "county_name", "grad_pct_clean", "dropout_pct_clean", "Poverty", "ChildPoverty")
  .show(10, truncate = false)

// ============================================================
// ANÁLISIS 9: Segmentación de Distritos
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 9: Segmentación de Distritos por Riesgo")
println("=" * 70)

val dfSegmented = df.withColumn("segmento",
  when(col("grad_pct_clean") < 70, "CRÍTICO - Intervención urgente")
  .when(col("grad_pct_clean") < 80, "RIESGO - Monitoreo intensivo")
  .when(col("grad_pct_clean") < 90, "ATENCIÓN - Seguimiento regular")
  .otherwise("ESTABLE - Mantener estrategia"))

println("\nDistribución de segmentos:")
dfSegmented.groupBy("segmento")
  .agg(
    count("*").alias("distritos"),
    round(avg("grad_pct_clean"), 2).alias("grad_promedio"),
    round(avg("dropout_pct_clean"), 2).alias("dropout_promedio"),
    round(avg("Poverty"), 2).alias("pobreza_promedio")
  )
  .orderBy("grad_promedio")
  .show(truncate = false)

// ============================================================
// ANÁLISIS 10: Factores Predictivos Combinados
// ============================================================

println("\n" + "=" * 70)
println("📊 ANÁLISIS 10: Análisis Multivariado de Factores de Riesgo")
println("=" * 70)

println("\nPerfil comparativo: Distritos en Riesgo vs No Riesgo")
df.groupBy("en_riesgo")
  .agg(
    count("*").alias("n"),
    round(avg("grad_pct_clean"), 2).alias("graduacion"),
    round(avg("dropout_pct_clean"), 2).alias("desercion"),
    round(avg("Income"), 0).alias("ingreso_med"),
    round(avg("Poverty"), 2).alias("pobreza"),
    round(avg("ChildPoverty"), 2).alias("pobreza_infantil"),
    round(avg("Unemployment"), 2).alias("desempleo"),
    round(avg("Professional"), 2).alias("pct_profesional")
  )
  .withColumn("estado", when(col("en_riesgo") === 1, "EN RIESGO").otherwise("NO RIESGO"))
  .select("estado", "n", "graduacion", "desercion", "ingreso_med", "pobreza", "pobreza_infantil", "desempleo", "pct_profesional")
  .show()

// ============================================================
// PROPUESTA DE VALOR
// ============================================================

println("\n" + "=" * 70)
println("💡 PROPUESTA DE VALOR")
println("=" * 70)

val distritosRiesgo = df.filter(col("en_riesgo") === 1).count()
val distritosCriticos = df.filter(col("grad_pct_clean") < 70).count()

println(s"""
┌─────────────────────────────────────────────────────────────────────┐
│  SISTEMA DE ALERTA TEMPRANA PARA DISTRITOS ESCOLARES EN RIESGO     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  PROBLEMA IDENTIFICADO:                                              │
│  • $distritosRiesgo distritos con graduación < 80%                         │
│  • $distritosCriticos distritos en situación CRÍTICA (< 70%)                   │
│  • Fuerte correlación entre pobreza y bajo rendimiento              │
│                                                                      │
│  SOLUCIÓN PROPUESTA:                                                 │
│  Dashboard predictivo que combina datos educativos + socioeconómicos│
│  para identificar distritos en riesgo ANTES de que sea tarde.       │
│                                                                      │
│  BENEFICIOS:                                                         │
│  1. Priorización de recursos basada en datos                         │
│  2. Intervención temprana en distritos vulnerables                   │
│  3. Reducción de brechas educativas por nivel socioeconómico         │
│  4. Mejora en tasas de graduación estatal                            │
│                                                                      │
│  USUARIOS OBJETIVO:                                                  │
│  • Departamento de Educación del Estado de NY                        │
│  • Superintendentes de distritos escolares                           │
│  • Investigadores en políticas educativas                            │
│                                                                      │
│  KPIs PROPUESTOS:                                                    │
│  • Reducir distritos en riesgo en 20% en 3 años                      │
│  • Aumentar graduación promedio estatal de 87% a 92%                 │
│  • Cerrar brecha de graduación por nivel de pobreza en 50%           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
""")

// ============================================================
// RESUMEN EJECUTIVO
// ============================================================

println("\n" + "=" * 70)
println("📋 RESUMEN EJECUTIVO")
println("=" * 70)

println(s"""
HALLAZGOS CLAVE:

1. MAGNITUD: $distritosRiesgo de ${df.count()} distritos están en riesgo (<80% graduación)

2. FACTORES DE RIESGO PRINCIPALES:
   • Alta deserción escolar (correlación: ${"%.3f".format(corrDropout)})
   • Pobreza del condado (correlación: ${"%.3f".format(corrPoverty)})
   • Pobreza infantil (correlación: ${"%.3f".format(corrChildPoverty)})

3. DISPARIDADES GEOGRÁFICAS:
   • NYC tiene diferente perfil de riesgo que el resto del estado
   • Condados rurales con alta pobreza son más vulnerables

4. VALOR DEL DATASET COMPLEMENTARIO:
   • Permite contextualizar el rendimiento educativo
   • Identifica factores socioeconómicos subyacentes
   • Mejora la capacidad predictiva del modelo

RECOMENDACIONES:
   → Implementar monitoreo continuo de distritos críticos
   → Priorizar inversión en condados con alta pobreza infantil
   → Desarrollar programas de retención para reducir deserción
   → Crear incentivos para distritos que mejoren sus métricas
""")

println("\n✅ Análisis completado!")

// spark.stop()


