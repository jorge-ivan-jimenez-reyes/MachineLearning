# Proyecto: Predicción de Distritos Escolares en Riesgo

## 1. Definición del Problema

### Contexto
Contamos con una aplicación que realiza predicciones de carreras STEM basándose en tests de inteligencias múltiples. Para complementar esta herramienta, necesitamos **identificar qué factores institucionales afectan el éxito académico de los estudiantes** antes de que lleguen a la universidad.

### ¿Por qué este dataset?
El dataset de tasas de graduación de New York (2021) nos permite:
- Entender qué características de un distrito predicen el éxito/fracaso estudiantil
- Identificar patrones que nuestra app podría usar para dar **recomendaciones personalizadas** según la región del estudiante
- Conectar factores socioeconómicos y geográficos con resultados educativos

### Conexión con nuestra aplicación
Si un estudiante proviene de un distrito "en riesgo", nuestra app podría:
- Ajustar las recomendaciones de carrera considerando barreras sistémicas
- Sugerir recursos adicionales de apoyo
- Identificar fortalezas que compensen desventajas del entorno

---

## 2. Objetivos

### Objetivo Principal
**Predecir si un distrito escolar está en riesgo de baja graduación (<80%)** para identificar estudiantes que podrían necesitar apoyo adicional.

### Objetivos Específicos
1. Identificar las variables más predictivas del éxito/fracaso escolar a nivel distrito
2. Construir un modelo de clasificación con F1-Score > 0.85
3. Analizar sesgos demográficos (raza, nivel económico, discapacidades)
4. Generar insights para integrar en nuestra app de predicción de carreras

---

## 3. Métricas de Evaluación

| Métrica | Definición | ¿Por qué importa? |
|---------|------------|-------------------|
| **F1-Score** | Balance entre Precision y Recall | Métrica principal (datos desbalanceados) |
| **Recall** | % de distritos en riesgo detectados | No queremos "perder" distritos problemáticos |
| **Precision** | % de predicciones correctas de riesgo | Evitar falsas alarmas |
| **AUC-ROC** | Capacidad de separar clases | Evaluar el modelo globalmente |

---

## 4. Dataset

### Fuente
- **Nombre:** GRAD_RATE_AND_OUTCOMES_2021.csv
- **Origen:** Departamento de Educación de New York
- **Registros:** 220,304 (filtrados a 4,040 distritos)
- **Columnas:** 36 features

### Target





### Balance de clases
- No riesgo (≥80%): 89.3%
- En riesgo (<80%): 10.7%

---

## 5. Análisis Exploratorio (EDA)

### 5.1 Limpieza de Datos

| Paso | Acción | Justificación |
|------|--------|---------------|
| 1 | Filtrar `aggregation_type = 'District'` | Evitar duplicados (Statewide, County, School) |
| 2 | Filtrar `subgroup_name = 'All Students'` | Un registro por distrito |
| 3 | Convertir porcentajes a numéricos | Columnas venían como "88%" (string) |
| 4 | Eliminar NaN en target | No podemos predecir sin valor real |

**Resultado:** 4,040 registros limpios

### 5.2 Detección de Outliers (Método IQR)

| Estadístico | Valor |
|-------------|-------|
| Q1 | 86% |
| Q3 | 95% |
| IQR | 9% |
| Límite inferior | 72.5% |
| Límite superior | 108.5% |
| **Outliers detectados** | 163 (4.0%) |

**Decisión:** Mantener outliers porque representan distritos reales con problemas severos que queremos predecir.

### 5.3 Análisis de Sesgos Demográficos

| Subgrupo | Tasa Graduación Promedio |
|----------|-------------------------|
| White | ~92% |
| All Students | ~88% |
| Hispanic or Latino | ~82% |
| Black or African American | ~80% |
| Economically Disadvantaged | ~78% |
| Students with Disabilities | ~68% |
| English Language Learner | ~55% |

**Sesgo identificado:** Estudiantes con discapacidades y ELL tienen tasas significativamente menores. Esto representa una brecha de equidad importante.

### 5.4 Análisis Geográfico

| Región | % en Riesgo |
|--------|-------------|
| NYC | 41.7% |
| Resto del estado | 9.1% |

**Insight:** NYC tiene 4.5x más probabilidad de tener distritos en riesgo.

---

## 6. Features Seleccionadas

| Feature | Correlación con Target | Justificación |
|---------|----------------------|---------------|
| `dropout_pct` | 0.54 | Más predictiva - alto abandono = alto riesgo |
| `still_enr_pct` | 0.44 | Estudiantes "estancados" sin graduarse |
| `reg_adv_pct` | -0.38 | Más diplomas avanzados = menor riesgo |
| `local_pct` | 0.02 | Tipo de diploma |
| `enroll_cnt` | 0.12 | Tamaño del distrito |
| `nyc_ind` | N/A | Diferencia geográfica significativa |

---

## 7. Modelo Seleccionado

### Random Forest 🏆

| Métrica | Valor |
|---------|-------|
| F1-Score (CV) | 0.88 |
| Estabilidad | Alta (menor varianza) |

### Feature Importance

| Feature | Importancia |
|---------|-------------|
| dropout_pct | 36% |
| still_enr_pct | 26% |
| reg_adv_pct | 22% |
| enroll_cnt | 9% |
| local_pct | 6% |
| nyc_ind | 1% |

---

## 8. Conclusiones

1. **El abandono escolar es el mejor predictor** de distritos en riesgo
2. **NYC requiere atención especial** con intervenciones focalizadas
3. **Existen sesgos demográficos** que afectan desproporcionadamente a minorías
4. **Random Forest** es el modelo más efectivo para esta tarea

### Aplicación en nuestra app:
- Usar ubicación del estudiante para ajustar recomendaciones
- Identificar estudiantes de distritos en riesgo para ofrecer recursos adicionales
- Considerar barreras sistémicas al sugerir carreras STEM