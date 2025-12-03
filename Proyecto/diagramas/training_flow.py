"""
Diagrama del Flujo de Entrenamiento - Random Forest
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.programming.language import Python
from diagrams.generic.storage import Storage
from diagrams.generic.compute import Rack
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

with Diagram("Flujo de Entrenamiento - Random Forest", 
             filename="training_flow",
             show=False,
             direction="TB"):
    
    # Datos
    with Cluster("Datos de Entrada"):
        raw = Storage("CSV Raw\n220,304 filas")
    
    # Preprocesamiento
    with Cluster("Preprocesamiento"):
        filter_data = Python("Filtrar\n- aggregation=District\n- subgroup=All Students")
        clean = Python("Limpiar\n- Convertir % a número\n- Eliminar NaN")
        target = Python("Crear Target\nen_riesgo = grad < 80%")
    
    # Features
    with Cluster("Feature Engineering"):
        features = Python("Features (6):\n• dropout_pct (36%)\n• still_enr_pct (26%)\n• reg_adv_pct (22%)\n• enroll_cnt (9%)\n• local_pct (5%)\n• nyc_ind (2%)")
    
    # Split
    with Cluster("Train/Test Split"):
        split = Python("80% Train\n20% Test\nstratify=y")
    
    # Modelos
    with Cluster("Entrenamiento de Modelos"):
        with Cluster("6 Modelos"):
            lr = Rack("Logistic\nF1: 0.77")
            dt = Rack("Decision Tree\nF1: 0.77")
            rf = Rack("Random Forest\nF1: 0.90 🏆")
            gb = Rack("Gradient Boost\nF1: 0.86")
            xgb = Rack("XGBoost\nF1: 0.88")
            svm = Rack("SVM\nF1: 0.77")
    
    # Optimización
    with Cluster("Optimización"):
        grid = Python("GridSearchCV\n12 combinaciones\ncv=5")
        best = Python("Best Params:\nn_estimators=200\nmax_depth=15")
    
    # Modelo final
    with Cluster("Modelo Final"):
        final = Python("Random Forest\nOptimizado\n✓ F1: 0.90\n✓ AUC: 0.99\n✓ 3 FP only")
    
    # Output
    with Cluster("Output"):
        pkl = Storage("modelo.pkl")
        report = Storage("classification_report")
    
    # Flujo
    raw >> filter_data >> clean >> target >> features >> split
    
    split >> lr
    split >> dt
    split >> rf
    split >> gb
    split >> xgb
    split >> svm
    
    rf >> Edge(label="mejor F1") >> grid >> best >> final
    
    final >> pkl
    final >> report

print("✅ Diagrama generado: training_flow.png")

