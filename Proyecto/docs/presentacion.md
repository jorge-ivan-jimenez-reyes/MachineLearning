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

## Objetivos y Cumplimiento

| # | Objetivo | Resultado | Estado |
|---|----------|-----------|--------|
| 1 | Predecir distritos en riesgo (<80% graduación) | Random Forest detecta 85% de distritos en riesgo | ✅ |
| 2 | Identificar variables más predictivas | dropout_pct (36%), still_enr_pct (26%), reg_adv_pct (22%) | ✅ |
| 3 | F1-Score > 0.85 | Random Forest: **F1 = 0.90**, CV = 0.898 | ✅ |
| 4 | Analizar sesgos demográficos | ELL (55%) y Discapacidades (68%) muy por debajo del promedio | ✅ |
| 5 | Generar insights para la app | Features claras para personalizar recomendaciones | ✅ |

---

## Modelos Utilizados

Probamos **6 modelos** para encontrar el mejor para datos desbalanceados (89% no riesgo vs 11% riesgo):

---

### 1. Logistic Regression

| Métrica | Valor |
|---------|-------|
| AUC-ROC | 0.982 |
| CV F1 | 0.773 |

**¿Qué hace?** Calcula probabilidad de riesgo con función lineal
**Fortaleza:** Interpretable - vemos el peso de cada variable
**Limitación:** No captura patrones complejos

---

### 2. Decision Tree

| Métrica | Valor |
|---------|-------|
| AUC-ROC | 0.970 |
| CV F1 | 0.772 |

**¿Qué hace?** Crea reglas: "Si dropout > 15% → Riesgo"
**Fortaleza:** Muy explicable
**Limitación:** Overfitting (memoriza datos)

---

### 3. Random Forest 🏆 GANADOR

| Métrica | Valor |
|---------|-------|
| AUC-ROC | **0.992** |
| CV F1 | **0.898** |
| Recall | 85% |

**¿Qué hace?** Combina 100 árboles y vota
**Fortaleza:** Mejor balance precisión/recall, muy estable
**Por qué ganó:** Detecta 73 de 86 distritos en riesgo con solo 3 falsos positivos

---

### 4. Gradient Boosting

| Métrica | Valor |
|---------|-------|
| AUC-ROC | 0.985 |
| CV F1 | 0.864 |

**¿Qué hace?** Árboles secuenciales que corrigen errores
**Fortaleza:** Captura patrones complejos
**Limitación:** Puede sobreajustar

---

### 5. XGBoost

| Métrica | Valor |
|---------|-------|
| AUC-ROC | 0.987 |
| CV F1 | 0.885 |

**¿Qué hace?** Versión optimizada de Gradient Boosting
**Fortaleza:** Maneja bien el desbalance de clases
**Usado para:** Validar resultados de Random Forest

---

### 6. SVM

| Métrica | Valor |
|---------|-------|
| AUC-ROC | 0.975 |
| CV F1 | 0.765 |

**¿Qué hace?** Encuentra superficie óptima para separar clases
**Fortaleza:** Robusto
**Limitación:** Lento, difícil de interpretar

---

## Comparación Final de Resultados

| Modelo | CV F1-Score | AUC-ROC | Ranking |
|--------|-------------|---------|---------|
| **Random Forest** | 0.898 ± 0.039 | 0.992 | 🥇 |
| **XGBoost** | 0.885 ± 0.036 | 0.987 | 🥈 |
| **Gradient Boosting** | 0.864 ± 0.035 | 0.985 | 🥉 |
| Logistic Regression | 0.773 ± 0.020 | 0.982 | 4 |
| Decision Tree | 0.772 ± 0.032 | 0.970 | 5 |
| SVM | 0.765 ± 0.020 | 0.975 | 6 |

---

## Matriz de Confusión - Random Forest

```
                 Predicho
              No riesgo  Riesgo
Real No riesgo    719       3      → 99.6% correcto
Real Riesgo        13      73      → 84.9% detectados
```

**Interpretación:**
- Solo **3 falsas alarmas** (distritos marcados como riesgo que no lo eran)
- **13 distritos en riesgo no detectados** (área de mejora)
- **73 de 86 distritos en riesgo correctamente identificados**

---

## Feature Importance - ¿Qué aprendió el modelo?

| Feature | Importancia | Coincide con EDA |
|---------|-------------|------------------|
| `dropout_pct` | **35.9%** | ✅ Correlación 0.54 |
| `still_enr_pct` | **25.6%** | ✅ Correlación 0.44 |
| `reg_adv_pct` | **22.4%** | ✅ Correlación -0.38 |
| `enroll_cnt` | 9.0% | ✅ Tamaño importa |
| `local_pct` | ~5% | ✅ Menor impacto |
| `nyc_ind` | ~1% | ⚠️ Ya capturado por otras variables |

**Conclusión:** El modelo aprendió exactamente lo que encontramos en el EDA. El **abandono escolar** es el mejor predictor de riesgo.

---

## Conclusiones

### ¿Qué modelo elegimos?
**Random Forest** porque:
1. ✅ Mejor F1-Score (0.898) - supera objetivo de 0.85
2. ✅ AUC-ROC más alto (0.992)
3. ✅ Más estable en cross-validation
4. ✅ Confirma hallazgos del EDA

### Aplicación en nuestra app de carreras STEM

| Si el estudiante... | La app puede... |
|--------------------|-----------------|
| Viene de distrito con alto dropout (>15%) | Mostrar programas de retención y becas |
| Su zona tiene pocos diplomas avanzados | Recomendar cursos de preparación STEM |
| Es de NYC | Conectar con recursos específicos de la ciudad |
| Tiene discapacidad o es ELL | Priorizar universidades con programas de accesibilidad |

---

## Próximos Pasos

1. Integrar el modelo en la app de predicción de carreras
2. Crear endpoint API para consultar riesgo por distrito
3. Diseñar interfaz para mostrar recomendaciones personalizadas
4. Validar con datos de otros estados

