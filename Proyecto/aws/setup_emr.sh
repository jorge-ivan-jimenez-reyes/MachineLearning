#!/bin/bash
# ============================================================
# SCRIPT DE CONFIGURACIÓN PARA AMAZON EMR
# Proyecto: Predicción de Distritos Escolares en Riesgo
# ============================================================

# ⚠️ CONFIGURAR ESTAS VARIABLES
BUCKET_NAME="proyecto-datos-masivos-$(whoami)"
REGION="us-east-1"
KEY_PAIR="tu-key-pair"  # Cambiar por tu key pair

echo "=============================================="
echo "🚀 CONFIGURACIÓN AMAZON EMR"
echo "=============================================="

# 1. Crear bucket S3
echo ""
echo "📦 Paso 1: Creando bucket S3..."
aws s3 mb s3://${BUCKET_NAME} --region ${REGION}

# 2. Subir datasets
echo ""
echo "📤 Paso 2: Subiendo datasets a S3..."
aws s3 cp ../data/GRAD_RATE_AND_OUTCOMES_2021.csv s3://${BUCKET_NAME}/data/
aws s3 cp ../data/acs2017_county_data.csv s3://${BUCKET_NAME}/data/

echo "✅ Datos subidos a s3://${BUCKET_NAME}/data/"

# 3. Subir notebook
echo ""
echo "📤 Paso 3: Subiendo notebook..."
aws s3 cp ../notebooks/proyecto_emr.ipynb s3://${BUCKET_NAME}/notebooks/

# 4. Crear cluster EMR
echo ""
echo "🖥️  Paso 4: Creando cluster EMR..."
echo "   (Esto puede tomar 5-10 minutos)"

aws emr create-cluster \
    --name "ProyectoDatosMasivos" \
    --release-label emr-6.10.0 \
    --applications Name=Spark Name=JupyterEnterpriseGateway Name=Livy \
    --instance-type m5.large \
    --instance-count 2 \
    --use-default-roles \
    --ec2-attributes KeyName=${KEY_PAIR} \
    --region ${REGION} \
    --log-uri s3://${BUCKET_NAME}/logs/ \
    --configurations '[
        {
            "Classification": "spark-defaults",
            "Properties": {
                "spark.driver.memory": "2g",
                "spark.executor.memory": "2g"
            }
        }
    ]'

echo ""
echo "=============================================="
echo "✅ CONFIGURACIÓN COMPLETADA"
echo "=============================================="
echo ""
echo "📋 Próximos pasos:"
echo "   1. Espera a que el cluster esté en estado 'Waiting'"
echo "   2. Ve a EMR → Notebooks → Create notebook"
echo "   3. Asocia el notebook al cluster creado"
echo "   4. Copia el contenido de proyecto_emr.ipynb"
echo "   5. Actualiza la variable S3_BUCKET con: s3://${BUCKET_NAME}"
echo ""
echo "📁 Rutas S3:"
echo "   • Datos: s3://${BUCKET_NAME}/data/"
echo "   • Notebook: s3://${BUCKET_NAME}/notebooks/"
echo "   • Output: s3://${BUCKET_NAME}/output/"

