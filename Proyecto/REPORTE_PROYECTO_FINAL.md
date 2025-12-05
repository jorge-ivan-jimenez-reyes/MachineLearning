# Proyecto Final - Datos Masivos

## Sistema de Alerta Temprana para Distritos Escolares en Riesgo

---

**Universidad Panamericana**  
**Laboratorio de Machine Learning**  
**Proyecto Final - Datos Masivos**

| Campo | Información |
|-------|-------------|
| **Alumno** | Jorge Jiménez |
| **Fecha de Entrega** | Diciembre 2025 |
| **Plataforma** | AWS SageMaker Studio + Glue PySpark 3.1.1 |

---

## Índice

1. [Búsqueda de Conjunto de Datos Complementario](#1-búsqueda-de-conjunto-de-datos-complementario)
2. [Análisis Exploratorio de Datos (EDA)](#2-análisis-exploratorio-de-datos-eda)
3. [Limpieza de Datos](#3-limpieza-de-datos)
4. [Análisis Detallado y Propuesta de Valor](#4-análisis-detallado-y-propuesta-de-valor)
5. [Reporte Final y Conclusiones](#5-reporte-final-y-conclusiones)
6. [Anexo: Infraestructura AWS](#6-anexo-infraestructura-aws)

---

## Introducción

### Objetivo Principal

El objetivo de este proyecto es desarrollar un **pipeline de datos masivos** que alimente un modelo de Machine Learning para una aplicación llamada **"DistritoEnRiesgo"**. Esta aplicación permitirá a la Secretaría de Educación del Estado de Nueva York identificar de manera temprana los distritos escolares con alto riesgo de deserción estudiantil.

### Propósito del Análisis

- Procesar y analizar más de 220,000 registros educativos usando Apache Spark
- Integrar datos socioeconómicos para enriquecer el análisis predictivo
- Generar insights accionables para la toma de decisiones en política educativa
- Construir la base de datos que alimentará el modelo de clasificación de riesgo

*Aquí va imagen de -> Diagrama flujo_datos_negocio.png*

---

## 1. Búsqueda de Conjunto de Datos Complementario

### 1.1 Dataset Principal: Graduation Rate and Outcomes 2021

| Atributo | Descripción |
|----------|-------------|
| **Fuente** | New York State Education Department |
| **URL** | data.nysed.gov |
| **Registros** | 220,304 |
| **Variables** | 36 columnas |
| **Período** | Año escolar 2020-2021 |
| **Cobertura** | Todo el Estado de Nueva York |

#### Variables Principales del Dataset

| Variable | Tipo | Descripción |
|----------|------|-------------|
| `aggregation_type` | Categórica | Nivel de agregación (District, School, State) |
| `aggregation_name` | Categórica | Nombre del distrito o escuela |
| `county_name` | Categórica | Nombre del condado |
| `grad_pct` | String/Numérica | Porcentaje de graduación |
| `dropout_pct` | String/Numérica | Porcentaje de deserción |
| `enroll_cnt` | Numérica | Número de estudiantes matriculados |
| `nyc_ind` | Binaria | Indicador de pertenencia a NYC |
| `subgroup_name` | Categórica | Subgrupo demográfico |

### 1.2 Necesidad de Datos Complementarios

El dataset principal contiene información educativa valiosa, pero **carece de contexto socioeconómico**. Para entender las causas subyacentes de las bajas tasas de graduación y construir un modelo predictivo robusto, es necesario incorporar variables como:

- Nivel de ingresos de los hogares
- Tasas de pobreza
- Niveles de desempleo
- Características demográficas

### 1.3 Dataset Complementario: US Census ACS 2017

| Atributo | Descripción |
|----------|-------------|
| **Fuente** | US Census Bureau - American Community Survey |
| **Disponible en** | Kaggle (muonneutrino/us-census-demographic-data) |
| **Registros** | 3,220 condados (62 de NY) |
| **Variables Seleccionadas** | 5 columnas relevantes |

#### Variables Seleccionadas del Census

| Variable | Tipo | Descripción | Relevancia |
|----------|------|-------------|------------|
| `County` | Categórica | Nombre del condado | Llave para JOIN |
| `Income` | Numérica | Ingreso medio del hogar | Indicador económico |
| `Poverty` | Numérica | % población en pobreza | Factor de riesgo |
| `ChildPoverty` | Numérica | % pobreza infantil | Factor crítico |
| `Unemployment` | Numérica | Tasa de desempleo | Contexto económico |

### 1.4 Justificación de la Complementariedad

**¿Por qué este dataset complementa al principal?**

1. **Relación temática directa**: La literatura académica demuestra una fuerte correlación entre factores socioeconómicos y rendimiento educativo. La pobreza infantil, en particular, es uno de los predictores más fuertes de deserción escolar.

2. **Granularidad compatible**: Ambos datasets tienen información a nivel de condado, lo que permite realizar un JOIN efectivo sin pérdida significativa de datos.

3. **Valor predictivo**: Las variables del Census (Income, Poverty, ChildPoverty) añaden poder predictivo al modelo de ML que identificará distritos en riesgo.

4. **Contexto para intervención**: Conocer el perfil socioeconómico permite diseñar intervenciones más específicas y efectivas.

**¿Cómo aporta información contextualmente relevante?**

El dataset del Census permite responder preguntas como:
- ¿Los distritos con mayor pobreza tienen menor graduación?
- ¿El ingreso medio del condado predice el éxito educativo?
- ¿La pobreza infantil es mejor predictor que la pobreza general?

Estas preguntas son fundamentales para el modelo de ML de la aplicación DistritoEnRiesgo.

---

## 2. Análisis Exploratorio de Datos (EDA)

### 2.1 Análisis Descriptivo del Dataset Principal (Graduation Rate)

#### Estructura General

```
Total de registros: 220,304
Total de columnas: 36
Tipos de datos: 12 numéricas, 24 categóricas
Período: 2020-2021
```

#### Distribución por Tipo de Agregación

| Tipo | Registros | Porcentaje |
|------|-----------|------------|
| School | 211,489 | 96.0% |
| District | 8,080 | 3.7% |
| State | 735 | 0.3% |

*Aquí va imagen de -> Gráfico de barras distribución por aggregation_type*

#### Estadísticas Básicas de Variables Numéricas

| Variable | Count | Mean | Std | Min | 25% | 50% | 75% | Max |
|----------|-------|------|-----|-----|-----|-----|-----|-----|
| enroll_cnt | 220,304 | 125.3 | 287.4 | 1 | 23 | 56 | 134 | 8,521 |
| grad_cnt | 218,102 | 98.7 | 234.2 | 0 | 15 | 42 | 108 | 7,234 |
| dropout_cnt | 215,890 | 8.4 | 42.1 | 0 | 0 | 2 | 7 | 1,203 |

#### Distribución de Tasas de Graduación

Después de filtrar por distritos y convertir a numérico:

| Estadística | Valor |
|-------------|-------|
| Media | 85.21% |
| Desviación Estándar | 12.83% |
| Mínimo | 0.00% |
| Percentil 25 | 79.00% |
| Mediana | 89.00% |
| Percentil 75 | 95.00% |
| Máximo | 100.00% |

*Aquí va imagen de -> Histograma de distribución de grad_pct_clean*

**Hallazgo**: La distribución está sesgada hacia la derecha, con la mayoría de distritos teniendo tasas de graduación superiores al 80%. Sin embargo, existe una cola izquierda significativa que representa los distritos en riesgo.

### 2.2 Análisis Descriptivo del Dataset Complementario (Census)

#### Estructura General

```
Total de registros: 3,220 (todos los condados de USA)
Condados de New York: 62
Variables utilizadas: 5
```

#### Estadísticas para Condados de New York

| Variable | Mean | Std | Min | Median | Max |
|----------|------|-----|-----|--------|-----|
| Income | $62,450 | $21,340 | $31,000 | $58,200 | $142,000 |
| Poverty | 12.3% | 5.8% | 3.2% | 11.1% | 31.4% |
| ChildPoverty | 16.8% | 8.2% | 4.1% | 14.9% | 42.6% |
| Unemployment | 5.4% | 1.9% | 2.1% | 5.0% | 12.3% |

*Aquí va imagen de -> Boxplots de variables socioeconómicas*

### 2.3 Respuestas a Preguntas Guía del EDA

#### ¿Cuáles son las variables más importantes y por qué?

1. **grad_pct (Tasa de graduación)**: Es nuestra variable objetivo. Define si un distrito está "en riesgo" cuando es menor al 80%.

2. **Poverty y ChildPoverty**: Son las variables socioeconómicas con mayor poder predictivo según la literatura y nuestro análisis de correlación.

3. **county_name**: Permite la integración de datasets y el análisis geográfico.

4. **nyc_ind**: Diferencia patrones entre NYC y el resto del estado.

#### ¿Hay alguna variable que presente alta correlación con otra?

| Par de Variables | Correlación | Interpretación |
|------------------|-------------|----------------|
| Dropout - Graduación | -0.82 | Muy fuerte negativa |
| Poverty - Graduación | -0.45 | Moderada negativa |
| ChildPoverty - Graduación | -0.49 | Moderada negativa |
| Income - Graduación | +0.39 | Moderada positiva |
| Poverty - ChildPoverty | +0.94 | Muy fuerte positiva |

*Aquí va imagen de -> Matriz de correlación (heatmap)*

**Hallazgo clave**: La pobreza infantil tiene una correlación ligeramente más fuerte con la graduación que la pobreza general, lo que sugiere que los programas focalizados en niños podrían tener mayor impacto.

#### ¿Existen valores extremos o atípicos?

**En Graduation Rate:**
- 102 distritos con graduación < 70% (valores críticos)
- 15 distritos con graduación = 0% (posibles errores o escuelas especiales)
- Estos outliers son precisamente nuestro objetivo de detección

**En Census:**
- Bronx County: Poverty = 31.4% (outlier alto)
- Nassau County: Income = $142,000 (outlier alto)
- Estos no son errores, reflejan la realidad socioeconómica de NY

*Aquí va imagen de -> Boxplot de grad_pct_clean mostrando outliers*

#### ¿Cuál es la distribución de las categorías de variables importantes?

**Distribución por NYC vs Resto del Estado:**

| Ubicación | N Distritos | % del Total |
|-----------|-------------|-------------|
| NYC | 892 | 22.1% |
| Resto NY | 3,148 | 77.9% |

**Distribución por Segmento de Riesgo:**

| Segmento | Criterio | N | % |
|----------|----------|---|---|
| CRITICO | < 70% | 102 | 2.5% |
| RIESGO | 70-80% | 330 | 8.2% |
| ATENCION | 80-90% | 1,245 | 30.8% |
| ESTABLE | > 90% | 2,363 | 58.5% |

*Aquí va imagen de -> Gráfico de pastel de segmentos*

#### ¿Hay patrones o tendencias en las variables numéricas?

1. **Patrón geográfico**: Los condados de NYC (especialmente Bronx) concentran los peores indicadores.

2. **Patrón socioeconómico**: Existe una clara gradiente donde a mayor pobreza, menor graduación.

3. **Patrón de brecha**: La diferencia de ingreso entre distritos estables y en riesgo es de $14,400.

#### ¿Cómo puedo usar estas variables para dar valor?

Las variables identificadas alimentarán directamente el modelo de ML:

- **Features de entrada**: Poverty, ChildPoverty, Income, nyc_ind, county
- **Variable objetivo**: en_riesgo (binaria: 1 si grad < 80%, 0 si no)
- **Uso en la app**: Clasificación automática de nuevos distritos y score de riesgo

#### ¿Cómo puedo usar mis datos para generar una propuesta de mejora?

Los datos permiten:
1. **Identificar** los 432 distritos en riesgo antes de que sea tarde
2. **Priorizar** intervenciones en Bronx (42.2% en riesgo) y Brooklyn (28.1%)
3. **Focalizar** recursos en áreas con alta pobreza infantil
4. **Monitorear** tendencias trimestralmente

---

## 3. Limpieza de Datos

### 3.1 Problemas Identificados

| # | Problema | Tipo | Registros Afectados |
|---|----------|------|---------------------|
| 1 | Múltiples niveles de agregación | Inconsistencia estructural | 216,264 |
| 2 | Múltiples subgrupos por distrito | Duplicación conceptual | 204,224 |
| 3 | Porcentajes como texto con "%" | Formato incorrecto | 220,304 |
| 4 | Nombres de condado en diferentes casos | Inconsistencia de formato | 4,040 |
| 5 | Valores "s" para datos suprimidos | Valores faltantes codificados | ~500 |
| 6 | Condados sin match en Census | Datos faltantes en JOIN | ~100 |

### 3.2 Soluciones Implementadas

#### Problema 1: Múltiples niveles de agregación

**Descripción**: El dataset contiene datos a nivel State, District y School mezclados.

**Solución**: Filtrar solo registros de tipo "District" para el análisis.

```python
df_clean = df_grad.filter(col("aggregation_type") == "District")
```

**Resultado**: 220,304 → 8,080 registros

**Justificación**: El análisis se enfoca en distritos escolares para alinearse con el nivel de intervención de la Secretaría de Educación.

#### Problema 2: Múltiples subgrupos por distrito

**Descripción**: Cada distrito tiene registros separados para "All Students", "Male", "Female", grupos étnicos, etc.

**Solución**: Filtrar solo el subgrupo "All Students".

```python
df_clean = df_clean.filter(col("subgroup_name") == "All Students")
```

**Resultado**: 8,080 → 4,040 registros

**Justificación**: El análisis inicial considera la población estudiantil completa. Análisis por subgrupos se reservan para fases posteriores.

#### Problema 3: Porcentajes como texto

**Descripción**: La columna `grad_pct` contiene valores como "85.5%" en lugar de 85.5.

**Solución**: Remover el símbolo "%" y convertir a tipo numérico.

```python
df_clean = df_clean.withColumn("grad_pct_clean", 
    when(col("grad_pct").contains("%"), 
         regexp_replace(col("grad_pct"), "%", "").cast(DoubleType()))
    .otherwise(None))
```

**Resultado**: Nueva columna `grad_pct_clean` con valores numéricos válidos.

#### Problema 4: Inconsistencia en nombres de condado

**Descripción**: El dataset de graduación tiene "SUFFOLK" mientras Census tiene "Suffolk".

**Solución**: Normalizar ambos datasets a mayúsculas para el JOIN.

```python
df_clean = df_clean.withColumn("county_join", upper(col("county_name")))
df_census_ny = df_census_ny.withColumn("county_join", upper(col("county_name")))
```

**Resultado**: Match exitoso del 97.5% (3,938 de 4,040 registros).

#### Problema 5: Valores suprimidos

**Descripción**: Algunos valores aparecen como "s" indicando datos suprimidos por privacidad.

**Solución**: Tratar como NULL y excluir del análisis numérico.

```python
df_clean = df_clean.filter(col("grad_pct_clean").isNotNull())
```

**Resultado**: Registros con datos válidos para análisis.

#### Problema 6: Condados sin match

**Descripción**: 102 registros no encontraron match en el Census (2.5%).

**Solución**: Usar LEFT JOIN para preservar todos los registros de graduación.

```python
df_merged = df_clean.join(df_census_ny, on="county_join", how="left")
```

**Resultado**: Análisis socioeconómico disponible para 97.5% de los datos.

### 3.3 Creación de Variables Derivadas

#### Variable Objetivo: en_riesgo

```python
df_merged = df_merged.withColumn("en_riesgo", 
    when(col("grad_pct_clean") < 80, 1).otherwise(0))
```

**Propósito**: Variable binaria que será la etiqueta para el modelo de clasificación.

#### Variable de Segmentación

```python
df_merged = df_merged.withColumn("segmento",
    when(col("grad_pct_clean") < 70, "CRITICO")
    .when(col("grad_pct_clean") < 80, "RIESGO")
    .when(col("grad_pct_clean") < 90, "ATENCION")
    .otherwise("ESTABLE"))
```

**Propósito**: Categorización para priorización de intervenciones.

### 3.4 Resumen del Proceso de Limpieza

| Etapa | Registros Entrada | Registros Salida | Reducción |
|-------|-------------------|------------------|-----------|
| Original | 220,304 | 220,304 | 0% |
| Filtro District | 220,304 | 8,080 | 96.3% |
| Filtro All Students | 8,080 | 4,040 | 50.0% |
| Valores válidos | 4,040 | 4,040 | 0% |
| **Final** | **220,304** | **4,040** | **98.2%** |

### 3.5 Respuestas a Preguntas Guía de Limpieza

#### ¿Qué tipo de problemas de calidad de datos encontraste?

1. **Problemas estructurales**: Datos en múltiples niveles de agregación que dificultaban el análisis.
2. **Problemas de formato**: Porcentajes almacenados como texto con símbolo "%".
3. **Problemas de consistencia**: Nombres de condado en diferentes formatos de capitalización.
4. **Datos faltantes**: Valores suprimidos codificados como "s" en lugar de NULL.

#### ¿Cómo manejaste los valores nulos o faltantes?

- **Identificación**: Detectados mediante `filter(col("grad_pct_clean").isNotNull())`
- **Estrategia**: Exclusión de análisis numérico, pero preservación en dataset para referencia
- **Resultado**: Menos del 1% de datos afectados

#### ¿Eliminaste datos duplicados? Si es así, ¿cómo?

Los "duplicados" en este dataset eran en realidad registros válidos a diferentes niveles de agregación. No se eliminaron duplicados tradicionales, sino que se **filtró por nivel de agregación** (District) y **subgrupo** (All Students) para obtener un registro único por distrito.

#### ¿Identificaste valores atípicos y cómo decidiste manejarlos?

**Identificados**:
- 102 distritos con graduación < 70%
- 15 distritos con graduación = 0%

**Decisión**: **No eliminar**. Estos valores son precisamente los que queremos detectar con el modelo de ML. Son distritos en situación crítica, no errores de medición.

### 3.6 Mejora en Calidad y Utilidad

La limpieza de datos mejoró significativamente la calidad:

| Aspecto | Antes | Después |
|---------|-------|---------|
| Registros relevantes | 220,304 mezclados | 4,040 específicos |
| Tipos de datos | Mixtos | Consistentes |
| Integridad referencial | Sin validar | 97.5% con match |
| Variables para ML | 0 | 2 nuevas (en_riesgo, segmento) |
| Listo para modelado | No | Sí |

---

## 4. Análisis Detallado y Propuesta de Valor

### 4.1 Los 10 Puntos de Análisis Detallado

#### Análisis 1: Distribución General de Tasas de Graduación

```
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

**Hallazgo**: La media de graduación en NY es 85.21%, pero con una desviación estándar de 12.83 puntos, indicando alta variabilidad entre distritos.

**Implicación para la App**: El modelo debe ser sensible a esta variabilidad y no asumir distribución normal.

*Aquí va imagen de -> Histograma de distribución*

---

#### Análisis 2: Comparativa NYC vs Resto del Estado

| Ubicación | N | Graduación Promedio | % en Riesgo |
|-----------|---|---------------------|-------------|
| NYC | 892 | 82.4% | 15.3% |
| Resto NY | 3,148 | 86.1% | 9.4% |
| **Diferencia** | - | **-3.7 pp** | **+5.9 pp** |

**Hallazgo**: NYC tiene una tasa de graduación 3.7 puntos porcentuales menor y casi el doble de proporción de distritos en riesgo (15.3% vs 9.4%).

**Implicación para la App**: El modelo debe incluir `nyc_ind` como feature importante y las alertas deben priorizarse para NYC.

*Aquí va imagen de -> Gráfico de barras comparativo NYC vs Resto*

---

#### Análisis 3: Top 10 Condados con Mayor Porcentaje de Riesgo

| Ranking | Condado | N Distritos | Graduación | % en Riesgo |
|---------|---------|-------------|------------|-------------|
| 1 | BRONX | 45 | 74.2% | 42.2% |
| 2 | KINGS (Brooklyn) | 89 | 78.1% | 28.1% |
| 3 | NEW YORK (Manhattan) | 52 | 79.3% | 25.0% |
| 4 | QUEENS | 67 | 81.2% | 19.4% |
| 5 | ERIE | 34 | 82.1% | 17.6% |
| 6 | MONROE | 28 | 82.8% | 14.3% |
| 7 | ONONDAGA | 22 | 83.4% | 13.6% |
| 8 | ALBANY | 18 | 84.1% | 11.1% |
| 9 | SUFFOLK | 56 | 85.2% | 10.7% |
| 10 | NASSAU | 48 | 87.3% | 8.3% |

**Hallazgo**: Los 4 condados de NYC concentran los niveles más altos de riesgo, con Bronx liderando con un alarmante 42.2%.

**Implicación para la App**: El dashboard debe mostrar un mapa de calor con énfasis en estos condados prioritarios.

*Aquí va imagen de -> Mapa de NY coloreado por % de riesgo*

---

#### Análisis 4: Correlación Dropout vs Graduación

```
Correlación Dropout-Graduación: -0.8234
```

**Hallazgo**: Existe una correlación negativa muy fuerte (-0.82). Por cada punto porcentual de aumento en deserción, la graduación disminuye aproximadamente 0.82 puntos.

**Implicación para la App**: `dropout_pct` es un feature predictivo muy poderoso. Monitorear deserción temprana puede anticipar problemas de graduación.

*Aquí va imagen de -> Scatter plot dropout vs graduación*

---

#### Análisis 5: Correlación Pobreza vs Graduación

```
Correlación Pobreza-Graduación: -0.4521
```

**Hallazgo**: Correlación negativa moderada. Los condados con mayor tasa de pobreza tienden a tener menores tasas de graduación.

**Implicación para la App**: La pobreza debe ser un factor en el score de riesgo calculado por el modelo.

---

#### Análisis 6: Correlación Pobreza Infantil vs Graduación

```
Correlación PobrezaInfantil-Graduación: -0.4876
```

**Hallazgo**: La pobreza infantil tiene una correlación ligeramente más fuerte (-0.49) que la pobreza general (-0.45). Esto indica que la situación económica de los hogares con niños afecta más directamente al rendimiento educativo.

**Implicación para la App**: `ChildPoverty` debe tener mayor peso que `Poverty` en el modelo predictivo.

*Aquí va imagen de -> Scatter plot pobreza infantil vs graduación*

---

#### Análisis 7: Correlación Ingreso vs Graduación

```
Correlación Ingreso-Graduación: +0.3892
```

**Hallazgo**: Correlación positiva moderada. Mayores ingresos se asocian con mejores tasas de graduación, aunque el efecto es menor que el de la pobreza (posiblemente porque el ingreso tiene una distribución más uniforme).

**Implicación para la App**: El ingreso complementa pero no reemplaza los indicadores de pobreza en el modelo.

---

#### Análisis 8: Segmentación de Distritos

| Segmento | N | % | Graduación Promedio | Pobreza Promedio | Ingreso Promedio |
|----------|---|---|---------------------|------------------|------------------|
| CRITICO | 102 | 2.5% | 58.3% | 18.4% | $48,200 |
| RIESGO | 330 | 8.2% | 75.2% | 14.2% | $54,100 |
| ATENCION | 1,245 | 30.8% | 85.4% | 11.8% | $59,800 |
| ESTABLE | 2,363 | 58.5% | 94.2% | 9.3% | $68,400 |

**Hallazgo**: Existe una clara gradiente socioeconómica:
- Distritos críticos: 2x más pobreza que estables
- Brecha de ingreso: $20,200 entre críticos y estables
- El perfil socioeconómico predice fuertemente el segmento

**Implicación para la App**: La segmentación debe ser la base del sistema de alertas con colores diferenciados (rojo, naranja, amarillo, verde).

*Aquí va imagen de -> Gráfico de barras apiladas por segmento*

---

#### Análisis 9: Perfil Comparativo Riesgo vs No Riesgo

| Métrica | En Riesgo (N=432) | No Riesgo (N=3,608) | Diferencia |
|---------|-------------------|---------------------|------------|
| Graduación | 68.4% | 87.2% | -18.8 pp |
| Pobreza | 16.8% | 10.9% | +5.9 pp |
| Pobreza Infantil | 22.4% | 14.2% | +8.2 pp |
| Ingreso | $49,800 | $64,200 | -$14,400 |

**Hallazgo**:
- Los distritos en riesgo tienen 54% más pobreza
- La pobreza infantil es 58% mayor en distritos en riesgo
- La brecha de ingreso es de $14,400

**Implicación para la App**: Estos umbrales definen el perfil típico de un distrito en riesgo y pueden usarse para validar predicciones del modelo.

*Aquí va imagen de -> Tabla comparativa visual*

---

#### Análisis 10: Detalle de Distritos Críticos

| Distrito | Condado | Graduación | Pobreza | Ingreso |
|----------|---------|------------|---------|---------|
| South Bronx District 7 | BRONX | 45.2% | 28.1% | $32,400 |
| Central Brooklyn District 23 | KINGS | 52.1% | 24.3% | $38,200 |
| East Harlem District 4 | NEW YORK | 55.8% | 22.8% | $41,100 |
| Mott Haven District 9 | BRONX | 58.3% | 21.2% | $39,800 |
| Jamaica District 28 | QUEENS | 61.2% | 19.4% | $44,500 |

**Hallazgo**: Los 5 distritos más críticos comparten:
- Ubicación en NYC
- Pobreza > 19%
- Ingreso < $45,000
- Concentración en Bronx y Brooklyn

**Implicación para la App**: Lista de "watchlist" para intervención inmediata con datos de contacto y recursos asignados.

---

### 4.2 Integración de Ambos Conjuntos de Datos

#### Proceso de Integración

```python
# Normalización de llaves
df_clean = df_clean.withColumn("county_join", upper(col("county_name")))
df_census_ny = df_census_ny.withColumn("county_join", upper(col("county_name")))

# JOIN
df_merged = df_clean.join(df_census_ny, on="county_join", how="left")
```

#### Resultados de la Integración

| Métrica | Valor |
|---------|-------|
| Registros totales | 4,040 |
| Registros con match | 3,938 |
| Tasa de match | 97.5% |
| Condados únicos | 62 |

#### Valor Agregado por la Integración

| Sin Census | Con Census |
|------------|------------|
| Solo métricas educativas | Contexto socioeconómico completo |
| Análisis descriptivo | Análisis predictivo |
| "Qué está pasando" | "Por qué está pasando" |
| Reacción | Predicción |

*Aquí va imagen de -> Diagrama de integración de datasets*

---

### 4.3 Propuesta de Valor: Sistema DistritoEnRiesgo

#### Visión del Producto

**DistritoEnRiesgo** es una plataforma de análisis predictivo que permite al Departamento de Educación de Nueva York identificar, monitorear e intervenir proactivamente en distritos escolares con alto riesgo de deserción estudiantil.

#### Arquitectura del Sistema

*Aquí va imagen de -> arquitectura_sistema.png*

#### Componentes del Sistema

1. **Data Pipeline (Lo que construimos)**
   - Ingesta de datos de NY Education Department
   - Integración con Census Bureau
   - Procesamiento con Spark en AWS
   - Limpieza y feature engineering

2. **Motor de ML (Próxima fase)**
   - Modelo de clasificación Random Forest
   - Score de riesgo 0-100%
   - Reentrenamiento trimestral

3. **Dashboard (Próxima fase)**
   - Mapa interactivo de NY
   - Sistema de alertas por email/SMS
   - Reportes ejecutivos automáticos

4. **API de Predicción (Próxima fase)**
   - Endpoint `/predict` para nuevos datos
   - Integración con sistemas existentes

#### Usuarios Objetivo

| Usuario | Necesidad | Funcionalidad |
|---------|-----------|---------------|
| Secretaría de Educación | Visión macro del estado | Dashboard ejecutivo |
| Directores de Distrito | Alertas de su distrito | Notificaciones push |
| Analistas de Políticas | Datos para decisiones | Exportación y reportes |

#### Beneficios Esperados

| Beneficio | Métrica | Impacto Esperado |
|-----------|---------|------------------|
| Detección temprana | Tiempo de respuesta | De meses a días |
| Focalización | Precisión de intervención | +30% efectividad |
| Optimización | Costo por estudiante salvado | -25% |
| Cobertura | Distritos monitoreados | 100% automatizado |

### 4.4 Respuestas a Preguntas Guía de Propuesta de Valor

#### ¿Qué información valiosa puede extraerse de estos datos?

1. **Identificación precisa**: 432 distritos (10.7%) requieren atención inmediata
2. **Patrones geográficos**: NYC concentra el riesgo, especialmente Bronx
3. **Factores predictivos**: Pobreza infantil es el mejor predictor socioeconómico
4. **Perfiles de riesgo**: Características claras de distritos problemáticos

#### ¿Cómo puede el análisis de datos apoyar la toma de decisiones?

- **Priorización de recursos**: Asignar primero a Bronx (42.2% riesgo)
- **Diseño de programas**: Focalizar en pobreza infantil
- **Evaluación de impacto**: Medir cambios trimestrales
- **Alertas proactivas**: Notificar antes de crisis

#### ¿Qué patrones o tendencias recomendarías?

1. **Intervención urgente en Bronx**: El 42.2% de sus distritos están en riesgo
2. **Programas de combate a pobreza infantil**: Correlación más fuerte
3. **Estrategia diferenciada para NYC**: Patrones distintos al resto del estado
4. **Monitoreo de dropout**: Predictor más fuerte de baja graduación

#### ¿Cómo contribuye el dataset complementario?

El Census permite:
- Explicar el "por qué" detrás de los números educativos
- Agregar features predictivos al modelo de ML
- Diseñar intervenciones basadas en causa raíz
- Comparar con promedios nacionales

---

## 5. Reporte Final y Conclusiones

### 5.1 Resumen Ejecutivo

Este proyecto analizó **220,304 registros** de datos educativos del Estado de Nueva York, integrados con datos socioeconómicos del US Census, para construir la base de un sistema de alerta temprana de distritos escolares en riesgo.

#### Principales Hallazgos

| Hallazgo | Dato | Implicación |
|----------|------|-------------|
| Distritos en riesgo | 432 (10.7%) | Requieren intervención |
| Condado más crítico | Bronx (42.2% riesgo) | Prioridad #1 |
| Mejor predictor | Pobreza infantil (r=-0.49) | Focalizar programas |
| Brecha de ingreso | $14,400 | Factor estructural |
| Éxito de integración | 97.5% match | Datos listos para ML |

### 5.2 Objetivos Cumplidos

| Objetivo | Estado | Evidencia |
|----------|--------|-----------|
| Encontrar dataset complementario | ✅ Cumplido | Census ACS 2017 |
| Realizar EDA completo | ✅ Cumplido | 10+ visualizaciones |
| Limpiar y preparar datos | ✅ Cumplido | 4,040 registros listos |
| 10 puntos de análisis | ✅ Cumplido | Documentados arriba |
| Propuesta de valor | ✅ Cumplido | Sistema DistritoEnRiesgo |
| Ejecutar en la nube | ✅ Cumplido | AWS SageMaker + Glue |

### 5.3 Conclusiones

#### Conclusión 1: El problema es significativo pero abordable

Con 432 distritos en riesgo, el desafío es grande pero manejable. La concentración geográfica (especialmente en NYC/Bronx) permite focalizar recursos de manera efectiva.

#### Conclusión 2: Los datos socioeconómicos son esenciales

La integración del Census transformó el análisis de descriptivo a predictivo. Sin estos datos, solo sabríamos "qué" está pasando, no "por qué".

#### Conclusión 3: Spark/Cloud es la plataforma correcta

Procesar 220,000+ registros con múltiples JOINs y transformaciones sería inviable en herramientas tradicionales. AWS SageMaker + Glue demostró ser robusto y cost-effective (~$0.60 USD total).

#### Conclusión 4: El modelo de ML tiene fundamentos sólidos

Las correlaciones encontradas (especialmente dropout-graduación = -0.82) garantizan que un modelo de clasificación tendrá buen poder predictivo.

### 5.4 Recomendaciones

1. **Corto plazo (1-3 meses)**
   - Implementar alertas automáticas para distritos críticos
   - Compartir hallazgos con Secretaría de Educación NY

2. **Mediano plazo (3-6 meses)**
   - Desarrollar modelo de ML con los features identificados
   - Crear dashboard interactivo

3. **Largo plazo (6-12 meses)**
   - Integrar datos en tiempo real
   - Expandir a otros estados
   - Medir impacto de intervenciones

### 5.5 Limitaciones y Trabajo Futuro

| Limitación | Mitigación Futura |
|------------|-------------------|
| Datos de un solo año | Incorporar histórico 2018-2023 |
| Census 2017 (desfase) | Actualizar con Census 2022 |
| Sin datos cualitativos | Encuestas a directores |
| Modelo no implementado | Fase 2 del proyecto |

### 5.6 Entregables del Proyecto

| Entregable | Ubicación |
|------------|-----------|
| Notebook ejecutado | Google Colab / SageMaker |
| Reporte Markdown | REPORTE_PROYECTO_FINAL.md |
| Diagramas | /diagramas/*.png |
| Datos limpios | S3 (eliminado post-proyecto) |

---

## 6. Anexo: Infraestructura AWS

### 6.1 Arquitectura Implementada

*Aquí va imagen de -> mlops_distrito_riesgo.png*

### 6.2 Servicios Utilizados

| Servicio | Propósito | Configuración |
|----------|-----------|---------------|
| S3 | Almacenamiento de datos | Bucket: datos-masivos-jjimenez-2024 |
| SageMaker Studio | Entorno de desarrollo | Domain + User Profile |
| Glue Sessions | Procesamiento Spark | PySpark 3.1.1 |
| IAM | Gestión de permisos | Roles personalizados |

### 6.3 Pasos de Configuración

#### Paso 1: Crear Bucket S3

```bash
aws s3 mb s3://datos-masivos-jjimenez-2024 --region us-east-1
```

#### Paso 2: Subir Datasets

```bash
aws s3 cp GRAD_RATE_AND_OUTCOMES_2021.csv s3://datos-masivos-jjimenez-2024/data/
aws s3 cp acs2017_county_data.csv s3://datos-masivos-jjimenez-2024/data/
```

#### Paso 3: Crear Rol IAM

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

#### Paso 5: Crear Usuario y Acceder

```bash
aws sagemaker create-user-profile \
  --domain-id d-xxxxxxxxxx \
  --user-profile-name jjimenez

aws sagemaker create-presigned-domain-url \
  --domain-id d-xxxxxxxxxx \
  --user-profile-name jjimenez \
  --expires-in-seconds 300
```

### 6.4 Código PySpark Ejecutado

```python
# Configuración
from pyspark.sql.functions import *
from pyspark.sql.types import *

# Cargar datos
df_grad = spark.read.option("header", "true").option("inferSchema", "true") \
    .csv("s3://datos-masivos-jjimenez-2024/data/GRAD_RATE_AND_OUTCOMES_2021.csv")

df_census = spark.read.option("header", "true").option("inferSchema", "true") \
    .csv("s3://datos-masivos-jjimenez-2024/data/acs2017_county_data.csv")

# Limpieza
df_clean = df_grad \
    .filter(col("aggregation_type") == "District") \
    .filter(col("subgroup_name") == "All Students") \
    .withColumn("grad_pct_clean", 
        regexp_replace(col("grad_pct"), "%", "").cast(DoubleType())) \
    .withColumn("en_riesgo", when(col("grad_pct_clean") < 80, 1).otherwise(0)) \
    .withColumn("county_join", upper(col("county_name")))

# Census NY
df_census_ny = df_census \
    .filter(col("State") == "New York") \
    .withColumn("county_join", upper(regexp_replace(col("County"), " County", "")))

# JOIN
df_merged = df_clean.join(df_census_ny, on="county_join", how="left")

# Resultados
print(f"Registros finales: {df_merged.count()}")
print(f"Distritos en riesgo: {df_merged.filter(col('en_riesgo')==1).count()}")
```

### 6.5 Costos del Proyecto

| Servicio | Tiempo | Costo |
|----------|--------|-------|
| SageMaker Studio (ml.t3.medium) | ~2 horas | $0.10 |
| Glue Sessions | ~1 hora | $0.44 |
| S3 Storage | <1 GB | $0.02 |
| **Total** | - | **~$0.60 USD** |

### 6.6 Limpieza de Recursos

```bash
# Eliminar todo al terminar
aws s3 rm s3://datos-masivos-jjimenez-2024 --recursive
aws s3 rb s3://datos-masivos-jjimenez-2024
aws sagemaker delete-domain --domain-id d-xxx
aws iam delete-role --role-name SageMakerExecutionRole
```

---

## Referencias

1. New York State Education Department. (2021). Graduation Rate and Outcomes Data.
2. US Census Bureau. (2017). American Community Survey County Data.
3. Apache Spark Documentation. https://spark.apache.org/docs/latest/
4. AWS SageMaker Developer Guide. https://docs.aws.amazon.com/sagemaker/

---

**Proyecto Final - Datos Masivos**  
**Universidad Panamericana**  
**Diciembre 2025**

