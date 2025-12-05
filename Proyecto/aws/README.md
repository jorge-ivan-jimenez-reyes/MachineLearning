# 🚀 Despliegue en Amazon EMR

## Guía Rápida

### Pre-requisitos
- AWS CLI configurado (`aws configure`)
- Cuenta AWS con permisos para EMR y S3
- Key pair de EC2 creado

---

## Opción 1: Script Automático

```bash
cd aws
chmod +x setup_emr.sh
./setup_emr.sh
```

---

## Opción 2: Paso a Paso Manual

### 1️⃣ Crear bucket S3

```bash
aws s3 mb s3://proyecto-datos-masivos-tunombre
```

### 2️⃣ Subir datos

```bash
aws s3 cp data/GRAD_RATE_AND_OUTCOMES_2021.csv s3://proyecto-datos-masivos-tunombre/data/
aws s3 cp data/acs2017_county_data.csv s3://proyecto-datos-masivos-tunombre/data/
```

### 3️⃣ Crear cluster EMR (Consola AWS)

1. Ir a **EMR** → **Create cluster**
2. Configurar:
   - **Name:** ProyectoDatosMasivos
   - **Release:** emr-6.10.0
   - **Applications:** Spark, JupyterEnterpriseGateway
   - **Instance type:** m5.large (suficiente para 220K registros)
   - **Instance count:** 2 (1 master + 1 core)
3. Click **Create cluster**

### 4️⃣ Crear notebook EMR

1. En EMR → **Notebooks** → **Create notebook**
2. Seleccionar el cluster creado
3. Kernel: **PySpark**
4. Copiar contenido de `notebooks/proyecto_emr.ipynb`

### 5️⃣ Configurar rutas en el notebook

```python
# Actualizar esta línea con tu bucket
S3_BUCKET = "s3://proyecto-datos-masivos-tunombre"
```

### 6️⃣ Ejecutar todas las celdas 🎉

---

## 📁 Estructura en S3

```
s3://tu-bucket/
├── data/
│   ├── GRAD_RATE_AND_OUTCOMES_2021.csv
│   └── acs2017_county_data.csv
├── notebooks/
│   └── proyecto_emr.ipynb
├── output/
│   ├── merged_data_csv/
│   └── merged_data_parquet/
└── logs/
```

---

## 💰 Costos Estimados

| Recurso | Tipo | Costo/hora |
|---------|------|------------|
| EMR Master | m5.large | ~$0.10 |
| EMR Core | m5.large | ~$0.10 |
| **Total cluster** | 2 nodos | ~$0.20/hora |

💡 Para 220K registros, 2 nodos m5.large son más que suficientes.

⚠️ **Importante:** Termina el cluster cuando no lo uses para evitar cargos.

```bash
# Terminar cluster
aws emr terminate-clusters --cluster-ids j-XXXXXXXXXXXXX
```

---

## 🔧 Troubleshooting

### Error: "Access Denied" en S3
```bash
# Verificar permisos del rol EMR
aws iam get-role --role-name EMR_DefaultRole
```

### Cluster no inicia
- Verificar límites de EC2 en tu cuenta
- Probar con instancias más pequeñas (m5.large)

### Notebook no conecta
- Esperar a que cluster esté en "Waiting"
- Verificar Security Groups permiten acceso

