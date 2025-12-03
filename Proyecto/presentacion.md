# Predicción de Distritos Escolares en Riesgo

---

## ¿Por qué este dataset?

Un estudiante brillante en matemáticas puede **NO llegar a una carrera STEM** si:
- Viene de un distrito con alto abandono
- No hay programas de diplomas avanzados
- Está en una zona con bajas tasas de graduación

| Dato | Valor |
|------|-------|
| Nombre | GRAD_RATE_AND_OUTCOMES_2021.csv |
| Origen | Departamento de Educación de New York |
| Registros | 220,304 (filtrados a 4,040 distritos) |
| Columnas | 36 features |

---

## ¿Qué valores del dataset nos ayudan?

| Columna | Qué significa | ¿Por qué importa? |
|---------|---------------|-------------------|
| `grad_pct` | % de estudiantes que se gradúan | **TARGET** - Lo que queremos predecir |
| `dropout_pct` | % que abandona la escuela | Si es alto → distrito en riesgo |
| `still_enr_pct` | % que sigue inscrito sin graduarse | Estudiantes "estancados" |
| `reg_adv_pct` | % con diploma Regents Avanzado | Si es alto → distrito exitoso |
| `local_pct` | % con diploma local (más básico) | Menos preparación para universidad |
| `enroll_cnt` | Cantidad de estudiantes | Tamaño del distrito |
| `nyc_ind` | ¿Está en NYC? (1=Sí, 0=No) | NYC tiene 4x más riesgo |
| `county_name` | Condado | Diferencias geográficas |
| `subgroup_name` | Grupo demográfico | Detectar sesgos (raza, discapacidad) |

---

## Hallazgos del EDA

### Correlaciones encontradas
| Hallazgo | Aplicación |
|----------|------------|
| Alto dropout = riesgo | Si el estudiante viene de zona con alto dropout → sugerir programas de retención |
| Pocos diplomas avanzados = riesgo | Si su distrito tiene pocos Regents Avanzados → recomendar cursos extra de preparación |

### Sesgos demográficos detectados
```
White                      → 92% graduación
All Students               → 88% graduación
Hispanic/Latino            → 82% graduación
Black/African American     → 80% graduación
Economically Disadvantaged → 78% graduación
Students with Disabilities → 68% graduación  
English Language Learner   → 55% graduación
```

---

## Objetivos

1. **Predecir** si un distrito escolar está en riesgo de baja graduación (<80%) para identificar estudiantes que podrían necesitar apoyo adicional
2. **Identificar** las variables más predictivas del éxito/fracaso escolar a nivel distrito
3. **Construir** un modelo de clasificación con F1-Score > 0.85
4. **Analizar** sesgos demográficos (raza, nivel económico, discapacidades)
5. **Generar insights** para integrar en nuestra app de predicción de carreras

---

## Modelos Utilizados

### ¿Por qué probamos 5 modelos?

Cada modelo tiene fortalezas diferentes. Los comparamos para encontrar el mejor para **nuestro problema específico**: detectar distritos en riesgo con datos desbalanceados (89% no riesgo vs 11% riesgo).

---

### 1. Logistic Regression

| Aspecto | Detalle |
|---------|---------|
| **Qué hace** | Calcula la probabilidad de riesgo usando una función lineal |
| **Basado en el EDA** | Usamos las correlaciones que encontramos (dropout 0.54, still_enr 0.44) |
| **Fortaleza** | Simple e interpretable - podemos ver el peso de cada variable |
| **Debilidad** | Asume relaciones lineales, puede perder patrones complejos |
| **Nos ayuda a** | Entender CUÁNTO afecta cada variable (coeficientes) |

**Ejemplo de uso:**
> "Si dropout sube 10%, la probabilidad de riesgo sube X%"

---

### 2. Decision Tree

| Aspecto | Detalle |
|---------|---------|
| **Qué hace** | Crea reglas de decisión tipo "Si dropout > 15% Y reg_adv < 20% → Riesgo" |
| **Basado en el EDA** | Usa los umbrales que identificamos (ej: <80% = riesgo) |
| **Fortaleza** | Muy explicable - podemos mostrar el árbol de decisiones |
| **Debilidad** | Puede memorizar los datos (overfitting) |
| **Nos ayuda a** | Crear **reglas claras** para identificar riesgo |

**Ejemplo de uso:**
> "Si un distrito tiene >20% dropout Y <25% diplomas avanzados → En riesgo"

---

### 3. Random Forest 🏆 (GANADOR)

| Aspecto | Detalle |
|---------|---------|
| **Qué hace** | Combina 100 árboles de decisión y vota la respuesta |
| **Basado en el EDA** | Aprovecha TODAS las variables que encontramos importantes |
| **Fortaleza** | Mejor F1-Score (0.88), más estable, maneja desbalance |
| **Debilidad** | Más lento, menos interpretable que un solo árbol |
| **Nos ayuda a** | **Predicción más precisa** + ver importancia de features |

**Resultados:**
- F1-Score: **0.88** ✅
- Feature más importante: `dropout_pct` (36%)
- Esto confirma lo que vimos en el EDA: el abandono es el mejor predictor

---

### 4. Gradient Boosting

| Aspecto | Detalle |
|---------|---------|
| **Qué hace** | Crea árboles secuenciales donde cada uno corrige errores del anterior |
| **Basado en el EDA** | Captura patrones complejos como la interacción NYC + dropout |
| **Fortaleza** | Alto AUC-ROC, captura relaciones no lineales |
| **Debilidad** | Puede sobreajustar si no se controla |
| **Nos ayuda a** | Detectar **patrones complejos** que otros modelos pierden |

**Ejemplo de uso:**
> "En NYC, un dropout de 15% es más grave que en zonas rurales" (interacción)

---

### 5. SVM (Support Vector Machine)

| Aspecto | Detalle |
|---------|---------|
| **Qué hace** | Encuentra la mejor línea/superficie para separar riesgo vs no riesgo |
| **Basado en el EDA** | Usa los datos normalizados (StandardScaler) |
| **Fortaleza** | Robusto cuando hay muchas variables |
| **Debilidad** | Lento, difícil de interpretar |
| **Nos ayuda a** | Verificar si los resultados son consistentes con otros modelos |

---

## Comparación de Resultados

| Modelo | F1-Score | ¿Por qué este resultado? |
|--------|----------|--------------------------|
| **Random Forest** | 0.88 | Combina múltiples árboles, reduce varianza |
| **Gradient Boosting** | 0.86 | Aprende de errores, captura patrones complejos |
| **Logistic Regression** | 0.78 | Relaciones lineales no capturan toda la complejidad |
| **Decision Tree** | 0.78 | Un solo árbol puede memorizar ruido |
| **SVM** | 0.76 | Más difícil de optimizar para datos desbalanceados |

---

## Feature Importance (Random Forest)

Lo que el modelo aprendió coincide con nuestro EDA:

| Feature | Importancia | Coincide con EDA |
|---------|-------------|------------------|
| `dropout_pct` | 36% | ✅ Correlación más alta (0.54) |
| `still_enr_pct` | 26% | ✅ Segunda correlación (0.44) |
| `reg_adv_pct` | 22% | ✅ Correlación negativa (-0.38) |
| `enroll_cnt` | 9% | ✅ Tamaño importa |
| `local_pct` | 6% | ✅ Menor impacto |
| `nyc_ind` | 1% | ⚠️ Bajo porque otras variables ya capturan el efecto NYC |

---

## Conclusión

### ¿Qué modelo elegimos?
**Random Forest** porque:
1. Mejor F1-Score (0.88) - supera nuestro objetivo de 0.85
2. Más estable en cross-validation
3. Confirma los hallazgos del EDA

### ¿Cómo aplicamos esto?
| Si el estudiante... | Nuestra app puede... |
|--------------------|---------------------|
| Viene de distrito con alto dropout | Mostrar programas de retención y apoyo |
| Su zona tiene pocos diplomas avanzados | Recomendar cursos de preparación extra |
| Es de NYC | Conectar con recursos específicos de la ciudad |
| Tiene discapacidad o es ELL | Priorizar universidades con buenos programas de accesibilidad |

---

## Siguiente paso
Integrar el modelo en la app de predicción de carreras STEM para dar recomendaciones personalizadas según el contexto del estudiante.

