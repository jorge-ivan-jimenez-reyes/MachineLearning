"""
Diagrama de Flujo de Datos a Decisión de Negocio
Proyecto DistritoEnRiesgo - De datos a acción
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.generic.storage import Storage
from diagrams.generic.compute import Rack
from diagrams.generic.database import SQL
from diagrams.generic.device import Mobile
from diagrams.programming.language import Python
from diagrams.onprem.client import Users, Client
from diagrams.aws.ml import Sagemaker
from diagrams.aws.storage import S3
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

# Diagrama 3: Flujo de Datos a Negocio
with Diagram("Flujo: Datos → Análisis → Decisión", 
             filename="flujo_datos_negocio",
             show=False, 
             direction="TB",
             graph_attr={"fontsize": "20", "bgcolor": "white"}):

    # Etapa 1: Datos
    with Cluster("ETAPA 1: DATOS"):
        with Cluster("Dataset Principal"):
            grad = Storage("Graduation Rate 2021\n220,304 registros\n• grad_pct\n• dropout_pct\n• county_name")
        with Cluster("Dataset Complementario"):
            census = Storage("US Census ACS 2017\n3,220 registros\n• Income\n• Poverty\n• ChildPoverty")
    
    # Etapa 2: Procesamiento
    with Cluster("ETAPA 2: PROCESAMIENTO (Spark)"):
        with Cluster("Limpieza"):
            filter_op = Python("Filtrar\nDistrict + All Students\n220K → 4,040")
        with Cluster("Transformación"):
            convert = Python("Convertir\n% → Numérico\nMAYÚSCULAS → Join")
        with Cluster("Integración"):
            join = Python("JOIN\nGrad + Census\n97.5% match")
        with Cluster("Feature Engineering"):
            features = Python("Crear Variables\n• en_riesgo (<80%)\n• segmento\n• county_join")
    
    # Etapa 3: Análisis
    with Cluster("ETAPA 3: ANÁLISIS"):
        with Cluster("10 Análisis Detallados"):
            stats = Python("Estadísticas\nDescriptivas")
            corr = Python("Correlaciones\nPobreza ↔ Grad")
            segment = Python("Segmentación\n4 categorías")
            geo = Python("Análisis\nGeográfico")
    
    # Etapa 4: ML
    with Cluster("ETAPA 4: MODELO ML"):
        train = Sagemaker("Entrenamiento\nRandom Forest")
        pred = Sagemaker("Predicción\nRiesgo 0-100%")
    
    # Etapa 5: Insights
    with Cluster("ETAPA 5: INSIGHTS"):
        insight1 = Storage("432 distritos\nen riesgo (10.7%)")
        insight2 = Storage("Bronx: 42.2%\nen riesgo")
        insight3 = Storage("Correlación\nPobreza-Grad: -0.45")
        insight4 = Storage("Brecha ingreso:\n$14,400")
    
    # Etapa 6: App
    with Cluster("ETAPA 6: APLICACIÓN"):
        with Cluster("DistritoEnRiesgo App"):
            app = Client("Dashboard\nInteractivo")
        with Cluster("Funcionalidades"):
            map_view = Mobile("Mapa de\nRiesgo")
            alerts_view = Mobile("Sistema de\nAlertas")
            reports_view = Mobile("Reportes\nAutomáticos")
    
    # Etapa 7: Decisión
    with Cluster("ETAPA 7: DECISIÓN DE NEGOCIO"):
        with Cluster("Secretaría de Educación NY"):
            decision1 = Users("Asignar recursos\na Bronx y Brooklyn")
            decision2 = Users("Programa especial\npobreza infantil")
            decision3 = Users("Monitoreo\ntrimestral")
    
    # Flujo
    grad >> filter_op
    census >> filter_op
    
    filter_op >> convert >> join >> features
    
    features >> stats
    features >> corr
    features >> segment
    features >> geo
    
    stats >> train
    corr >> train
    segment >> train
    geo >> train
    
    train >> pred
    
    pred >> insight1
    pred >> insight2
    pred >> insight3
    pred >> insight4
    
    insight1 >> app
    insight2 >> app
    insight3 >> app
    insight4 >> app
    
    app >> map_view
    app >> alerts_view
    app >> reports_view
    
    map_view >> decision1
    alerts_view >> decision2
    reports_view >> decision3

print("Diagrama generado: flujo_datos_negocio.png")

