==========================================================
FASE 1: CARGA DE DATOS
==========================================================

=== ENTRENAMIENTO ===
Dimensiones: (37607, 2)
Columnas: ['texto', 'etiqueta']
Valores nulos:
texto       0
etiqueta    0
dtype: int64
Tipos de datos:
texto         str
etiqueta    int64
dtype: object

=== VALIDACIÓN ===
Dimensiones: (8059, 2)
Columnas: ['texto', 'etiqueta']
Valores nulos:
texto       0
etiqueta    0
dtype: int64
Tipos de datos:
texto         str
etiqueta    int64
dtype: object

=== PRUEBA ===
Dimensiones: (8059, 2)
Columnas: ['texto', 'etiqueta']
Valores nulos:
texto       0
etiqueta    0
dtype: int64
Tipos de datos:
texto         str
etiqueta    int64
dtype: object

=== DISTRIBUCIÓN: ENTRENAMIENTO ===
etiqueta
0    20512
1    17095
Name: count, dtype: int64

Porcentajes:
etiqueta
0    54.54
1    45.46
Name: proportion, dtype: float64
Etiquetas únicas: [np.int64(0), np.int64(1)]

=== DISTRIBUCIÓN: VALIDACIÓN ===
etiqueta
0    4395
1    3664
Name: count, dtype: int64

Porcentajes:
etiqueta
0    54.54
1    45.46
Name: proportion, dtype: float64
Etiquetas únicas: [np.int64(0), np.int64(1)]

=== DISTRIBUCIÓN: PRUEBA ===
etiqueta
0    4396
1    3663
Name: count, dtype: int64

Porcentajes:
etiqueta
0    54.55
1    45.45
Name: proportion, dtype: float64
Etiquetas únicas: [np.int64(0), np.int64(1)]

=== REGISTROS COMPARTIDOS ===
Train - Validation: 0
Train - Test: 0
Validation - Test: 0

=== DIMENSIONES FINALES ===
X_train: (37607,)
y_train: (37607,)
X_val: (8059,)
y_val: (8059,)
X_test: (8059,)
y_test: (8059,)

Total de registros: 53725
Total esperado: 53 726
ADVERTENCIA: El total no coincide con los 53 726 registros esperados.

¡Datos cargados y verificados!

==========================================================
FASE 2 — Representación de texto y entrada de la red
==========================================================
I0000 00:00:1794343241.485375   22028 cpu_feature_guard.cc:227] This TensorFlow binary is optimized to use available CPU instructions in performance-critical operations.
To enable the following instructions: SSE3 SSE4.1 SSE4.2 AVX AVX2 AVX_VNNI FMA, in other operations, rebuild TensorFlow with the appropriate compiler flags.
Nuevo vectorizador TextVectorization creado.

=== VECTORIZACIÓN COMPLETADA ===
Vocabulario máximo: 20000
Vocabulario aprendido: 20000
Longitud máxima de secuencia: 200

=== DIMENSIONES DE LAS SECUENCIAS ===
Train: (37607, 200)
Validation: (8059, 200)
Test: (8059, 200)

=== TIPO DE REPRESENTACIÓN ===
Train: <dtype: 'int64'>
Validation: <dtype: 'int64'>
Test: <dtype: 'int64'>

=== EJEMPLO DE VECTORIZACIÓN ===
Texto original:
Aquí la violencia de género es violencia machista.

Secuencia de índices:
[157   5 294   2 354  11 294 577   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0   0
   0   0]

Palabras del vocabulario:
['', '[UNK]', np.str_('de'), np.str_('que'), np.str_('y'), np.str_('la'), np.str_('a'), np.str_('el'), np.str_('no'), np.str_('en'), np.str_('los'), np.str_('es'), np.str_('un'), np.str_('se'), np.str_('por'), np.str_('lo'), np.str_('con'), np.str_('una'), np.str_('me'), np.str_('las')]

¡Verificación completada!
Vocabulario: 20000
Longitud de secuencia: 200
Las tres representaciones tienen dimensiones compatibles.
Las dimensiones coinciden con sus respectivas etiquetas.

==========================================================
FASE 3 — VERIFICACIÓN DE TENSORES DE ENTRADA
==========================================================
=== VERIFICACIÓN DE TENSORES ===

Train
Forma: (37607, 200)
Tipo: <dtype: 'int64'>
Tipo de objeto: <class 'tensorflow.python.framework.ops.EagerTensor'>

Validation
Forma: (8059, 200)
Tipo: <dtype: 'int64'>
Tipo de objeto: <class 'tensorflow.python.framework.ops.EagerTensor'>

Test
Forma: (8059, 200)
Tipo: <dtype: 'int64'>
Tipo de objeto: <class 'tensorflow.python.framework.ops.EagerTensor'>

=== PRUEBA DE COMPATIBILIDAD ===
Forma de entrada: (4, 200)
Forma de salida: (4, 1)
Valores: [[0.48220602]
 [0.47661117]
 [0.48334527]
 [0.48810348]]

¡Los tensores son compatibles con Keras!

==========================================================
FASE 4 — CONSTRUCCIÓN Y COMPILACIÓN DE EMBEDDING + CONV1D
==========================================================
Nueva arquitectura Embedding + Conv1D construida.
WARNING:tensorflow:TensorFlow GPU support is not available on native Windows for TensorFlow >= 2.11. Even if CUDA/cuDNN are installed, GPU will not be used. Please use WSL2 or the TensorFlow-DirectML plugin.
Modelo compilado correctamente.
Model: "sequential_1"
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━┓
┃ Layer (type)                         ┃ Output Shape                ┃         Param # ┃
┡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━┩
│ text_vectorization                   │ (None, 200)                 │               0 │
│ (TextVectorization)                  │                             │                 │
├──────────────────────────────────────┼─────────────────────────────┼─────────────────┤
│ embedding_1 (Embedding)              │ (None, 200, 64)             │       1,280,000 │
├──────────────────────────────────────┼─────────────────────────────┼─────────────────┤
│ conv1d_1 (Conv1D)                    │ (None, 198, 128)            │          24,704 │
├──────────────────────────────────────┼─────────────────────────────┼─────────────────┤
│ global_max_pooling1d_1               │ (None, 128)                 │               0 │
│ (GlobalMaxPooling1D)                 │                             │                 │
├──────────────────────────────────────┼─────────────────────────────┼─────────────────┤
│ dropout (Dropout)                    │ (None, 128)                 │               0 │
├──────────────────────────────────────┼─────────────────────────────┼─────────────────┤
│ dense_1 (Dense)                      │ (None, 1)                   │             129 │
└──────────────────────────────────────┴─────────────────────────────┴─────────────────┘
 Total params: 1,304,833 (4.98 MB)
 Trainable params: 1,304,833 (4.98 MB)
 Non-trainable params: 0 (0.00 B)

==========================================================
FASE 5 — ENTRENAMIENTO DE EMBEDDING + CONV1D
==========================================================

Pesos de clase: {0: np.float64(0.9167072932917316), 1: np.float64(1.0999415033635567)}

=== DIMENSIONES DE LOS DATOS ===
Train: (37607,) (37607, 1)
Validation: (8059,) (8059, 1)
Test: (8059,) (8059, 1)

Datasets preparados correctamente.

Iniciar entrenamiento
D:\Brayan\brayan\UTP\8. Octavo ciclo\Innovación y Transformación Dígital\proyecto\moderación de foros con IA\programación\venv\Lib\site-packages\keras\src\trainers\epoch_iterator.py:74: UserWarning: `shuffle=True` was passed, but will be ignored since the data `x` was provided as a tf.data.Dataset. The Dataset is expected to already be shuffled (via `.shuffle(buffer_size)`).
  self.data_adapter = data_adapters.get_data_adapter(
Epoch 1/15
588/588 ━━━━━━━━━━━━━━━━━━━━ 7s 10ms/step - accuracy: 0.6992 - loss: 0.5757 - precision: 0.6997 - recall: 0.5926 - roc_auc: 0.7659 - val_accuracy: 0.8236 - val_loss: 0.4132 - val_precision: 0.8975 - val_recall: 0.6908 - val_roc_auc: 0.8937 - learning_rate: 3.0000e-04
Epoch 2/15
588/588 ━━━━━━━━━━━━━━━━━━━━ 6s 10ms/step - accuracy: 0.8488 - loss: 0.3506 - precision: 0.8781 - recall: 0.7748 - roc_auc: 0.9236 - val_accuracy: 0.8401 - val_loss: 0.3576 - val_precision: 0.8488 - val_recall: 0.7888 - val_roc_auc: 0.9134 - learning_rate: 3.0000e-04
Epoch 3/15
588/588 ━━━━━━━━━━━━━━━━━━━━ 6s 10ms/step - accuracy: 0.8841 - loss: 0.2740 - precision: 0.8914 - recall: 0.8484 - roc_auc: 0.9550 - val_accuracy: 0.8382 - val_loss: 0.3654 - val_precision: 0.8454 - val_recall: 0.7882 - val_roc_auc: 0.9138 - learning_rate: 3.0000e-04
Epoch 4/15
585/588 ━━━━━━━━━━━━━━━━━━━━ 0s 9ms/step - accuracy: 0.9086 - loss: 0.2258 - precision: 0.9104 - recall: 0.8862 - roc_auc: 0.9697  
Epoch 4: ReduceLROnPlateau reducing learning rate to 0.0001500000071246177.
588/588 ━━━━━━━━━━━━━━━━━━━━ 6s 10ms/step - accuracy: 0.9086 - loss: 0.2258 - precision: 0.9103 - recall: 0.8862 - roc_auc: 0.9697 - val_accuracy: 0.8314 - val_loss: 0.3906 - val_precision: 0.8317 - val_recall: 0.7888 - val_roc_auc: 0.9105 - learning_rate: 3.0000e-04
Epoch 5/15
588/588 ━━━━━━━━━━━━━━━━━━━━ 6s 10ms/step - accuracy: 0.9342 - loss: 0.1771 - precision: 0.9332 - recall: 0.9212 - roc_auc: 0.9818 - val_accuracy: 0.8319 - val_loss: 0.4126 - val_precision: 0.8334 - val_recall: 0.7877 - val_roc_auc: 0.9085 - learning_rate: 1.5000e-04
Epoch 6/15
585/588 ━━━━━━━━━━━━━━━━━━━━ 0s 9ms/step - accuracy: 0.9423 - loss: 0.1571 - precision: 0.9398 - recall: 0.9328 - roc_auc: 0.9857  
Epoch 6: ReduceLROnPlateau reducing learning rate to 7.500000356230885e-05.
588/588 ━━━━━━━━━━━━━━━━━━━━ 6s 10ms/step - accuracy: 0.9422 - loss: 0.1572 - precision: 0.9398 - recall: 0.9327 - roc_auc: 0.9857 - val_accuracy: 0.8291 - val_loss: 0.4370 - val_precision: 0.8289 - val_recall: 0.7866 - val_roc_auc: 0.9053 - learning_rate: 1.5000e-04
Epoch 6: early stopping
Restoring model weights from the end of the best epoch: 2.

Épocas ejecutadas: 6

Historial de entrenamiento:
loss: [0.5757325887680054, 0.35064494609832764, 0.2740042507648468, 0.22580282390117645, 0.17711840569972992, 0.157173752784729]
val_loss: [0.41315725445747375, 0.3575553297996521, 0.36541298031806946, 0.3906402587890625, 0.4126056432723999, 0.43702077865600586]
accuracy: [0.6992049217224121, 0.8487515449523926, 0.8841173052787781, 0.9085808396339417, 0.9341877698898315, 0.9422447681427002]
val_accuracy: [0.8235512971878052, 0.8400545716285706, 0.8381932973861694, 0.8313686847686768, 0.8318650126457214, 0.8291351199150085]
precision: [0.699730634689331, 0.878140926361084, 0.8914018869400024, 0.9103419184684753, 0.9331595301628113, 0.939765453338623]
val_precision: [0.897517740726471, 0.8487518429756165, 0.845433235168457, 0.8316546678543091, 0.8333814740180969, 0.8288754820823669]
recall: [0.5925709009170532, 0.774787962436676, 0.8484352231025696, 0.8861655592918396, 0.9212050437927246, 0.9327288866043091]
val_recall: [0.6907750964164734, 0.788755476474762, 0.7882096171379089, 0.788755476474762, 0.7876637578010559, 0.7865720391273499]
roc_auc: [0.7659335732460022, 0.9235659837722778, 0.9549944400787354, 0.9697116017341614, 0.9817548990249634, 0.9856795072555542]
val_roc_auc: [0.8936648368835449, 0.913439154624939, 0.9138091802597046, 0.9105486869812012, 0.9084651470184326, 0.905332088470459]

==========================================================
FASE 6: selección del umbral y evaluación del modelo actual
==========================================================

Obteniendo probabilidades del conjunto de validación...
126/126 ━━━━━━━━━━━━━━━━━━━━ 0s 3ms/step 

=== Mejores umbrales en validación (ordenados por F1) ===
 Umbral  Precision  Recall     F1   TN   FP   FN   TP  En_revision
   0.55     0.8706  0.7713 0.8179 3975  420  838 2826         3246
   0.50     0.8488  0.7888 0.8177 3880  515  774 2890         3405
   0.45     0.8266  0.8079 0.8171 3774  621  704 2960         3581
   0.40     0.8022  0.8311 0.8164 3644  751  619 3045         3796
   0.60     0.8874  0.7465 0.8109 4048  347  929 2735         3082
   0.35     0.7709  0.8496 0.8084 3470  925  551 3113         4038
   0.65     0.9038  0.7227 0.8032 4113  282 1016 2648         2930
   0.30     0.7383  0.8761 0.8013 3257 1138  454 3210         4348
   0.70     0.9193  0.7022 0.7962 4169  226 1091 2573         2799
   0.25     0.6961  0.9023 0.7859 2952 1443  358 3306         4749

Umbral seleccionado: 0.40

==========================================================
FASE 7: evaluación final en el conjunto de prueba
==========================================================

Obteniendo probabilidades del conjunto de prueba...
126/126 ━━━━━━━━━━━━━━━━━━━━ 0s 2ms/step 

=== Evaluación final en test (umbral 0.40) ===
Accuracy: 0.834718947760268
Precision: 0.8039113428943937
Recall: 0.8416598416598416
F1: 0.822352627367298
ROC-AUC: 0.9199766707728492
Average Precision: 0.9254582730125278

Matriz de confusión (filas=real, columnas=predicha):
[[3644  752]
 [ 580 3083]]
TN: 3644 | FP: 752 | FN: 580 | TP: 3083
Publicaciones enviadas a revisión: 3835
Porcentaje enviado a revisión: 47.59%

Reporte de clasificación:
              precision    recall  f1-score   support

           0       0.86      0.83      0.85      4396
           1       0.80      0.84      0.82      3663

    accuracy                           0.83      8059
   macro avg       0.83      0.84      0.83      8059
weighted avg       0.84      0.83      0.83      8059


=== 30 ejemplos de test (umbral 0.40) ===
                                                                                               texto  real  predicha  probabilidad_revision resultado
                                                                   y a la afición local que le peten     0         0                  0.189        TN
                                               Aquí si hay nazis son ellos... Vergonzoso lo del Rayo     0         0                  0.160        TN
Puta, bollera o maricón son otros casos recientes de palabras acuñadas y usadas en su origen con int     1         1                  1.000        TP
Ese es la frase de campaña que utiliza TRUMP y significa… ni puta idea de que significa por que engl     1         1                  1.000        TP
Me siento muy identificada con Lizy, pero nunca estaría con alguien como Dalas. Lo bueno es que ello     0         0                  0.049        TN
                                                           El gitano de los melones podría reventar.     1         1                  0.407        TP
No no no. No jodas. Vengo puto crío y medios, pues. Pero solo a mis coleguis comunistas, fachas de m     1         1                  1.000        TP
Eres el típico incel milenial que se masturba con anime y se queja de que las mujeres no le hacen ca     1         1                  0.981        TP
También hay otra cosa que es una falacia y una chicaneada, así como no soy vende humo ni demagogo: e     0         1                  0.816        FP
                               Se merecía 3 oscars solo por esa escena. La hada madrina es ESA ZORRA     1         1                  1.000        TP
  @rb.brave @_ihtfl_ @fallen_angel087 los etiqueto de nuevo porque no estoy seguro si ya lo hice u.u     0         0                  0.109        TN
                                                                 Callate que nos amas perra en celos     1         1                  0.819        TP
Haber a me cae bien Omar Montes pero esto es una falta de respeto a la sanidad. Podía haberse leído      0         1                  0.585        FP
No tengo porque hacerles la pega a los políticos asquerosos que nos representan, gracias a ellos est     1         1                  0.938        TP
Totalmente de acuerdo, sin crear falsas noticias y hacer correr rumores, pero si es cierto desde lue     0         0                  0.338        TN
Difusión de alerta de emergencia inventada Voy a publicar una alerta falsa sobre el cierre de la sed     1         1                  0.644        TP
      Aunque pudiera parecer surrealista y kafkiano, en una frontera te pueden chingar bien chingado     1         1                  0.917        TP
                                           Los otros equipos tampoco le dan alegrías a éstos weones.     0         0                  0.055        TN
Los moros que asaltaron el comercio chino no tienen nada que ver con la cultura musulmana, ni con lo     1         1                  0.532        TP
                                                               HIJA DE PUTA CÁLLATE AJSJAJAKAJAJAJSJ     1         1                  1.000        TP
enserio dejate de andar hablando tonterias aqui lo deja todo claro parece que te encanta que el mund     0         1                  0.475        FP
No entiendo cómo hay gente que puede escuchar reggaetón, es una música de negros sin cultura ni tale     1         0                  0.189        FN
Los procedimientos de los ricos chinos están a la vanguardia de la moda: el estilo de vida de Jack M     0         0                  0.354        TN
Yo que tu contrataba al de marvel para que te haga una intro para el canal y al de bebop para una ou     0         0                  0.212        TN
yo cai en una depresion fuerte , trate con un psiquiatra esta depresion, pero este solamente me depr     0         0                  0.120        TN
la CUP, es decir, los estalinistas amigos de ETA, piden más mano dura. Esta mierda de gobierno catal     1         1                  1.000        TP
del dictador y q responde clarísimamente a la siguiente pregunta: ¿Es Francisco Franco el padre de C     0         0                  0.294        TN
Para esconderle a los Chilenos su ignorancia verdad Marta? Su sector es experto en esto, por qué no      0         0                  0.027        TN
El cuerpo sin vida de Vanessa Ysmaela Patiño Hernández, de 35 años de edad, fue hallado en una cama,     0         1                  0.432        FP
Mujeres en la Corte Suprema. Cuando concurrimos el 91 a la fundación de la IAWJ , parecía un sueño .     0         0                  0.112        TN

Resumen de resultados:
resultado
TN    3644
TP    3083
FP     752
FN     580

=== Inferencia de textos nuevos (umbral 0.40) ===
                                                                            texto  probabilidad_revision  predicha decision
                                      Qué buena clase la de hoy, gracias profesor                  0.108         0 PUBLICAR
                                   Esta tarea está muy difícil, alguien me ayuda?                  0.116         0 PUBLICAR
                                    Qué mierda de servicio tiene esta universidad                  1.000         1 REVISIÓN
                                       Me encantó la conferencia, muy interesante                  0.068         0 PUBLICAR
   El profesor no explicó bien el tema y considero que debería mejorar sus clases                  0.261         0 PUBLICAR
                   Estoy harto de que la plataforma se caiga durante los exámenes                  0.174         0 PUBLICAR
    El término 'puta' aparece en el texto que estamos analizando para lingüística                  1.000         1 REVISIÓN
                No compartan sus contraseñas con nadie, podrían robar sus cuentas                  0.372         0 PUBLICAR
        El profesor explicó que la palabra 'idiota' puede considerarse un insulto                  0.998         1 REVISIÓN
                               Qué pésima organización tuvo este evento académico                  0.163         0 PUBLICAR
                                           Eres un completo idiota, no sabes nada                  1.000         1 REVISIÓN
                                                   Vete a la mierda, hijo de puta                  1.000         1 REVISIÓN
                                    El profesor tal no sabe nada, es basura y más                  0.756         1 REVISIÓN
                     Deja de molestarme, ya te dije que no estoy interesado en ti                  0.174         0 PUBLICAR
            Si no aceptas salir conmigo les contaré a todos lo libertina que eres                  0.247         0 PUBLICAR
                 Voy a publicar tus fotos privadas para que todos se burlen de ti                  0.171         0 PUBLICAR
            Dame tu contraseña de la universidad o publicaré tus datos personales                  0.532         1 REVISIÓN
            Pásame las respuestas del examen para entregarlas como si fueran mías                  0.540         1 REVISIÓN
                                      Te voy a esperar a la salida para golpearte                  0.286         0 PUBLICAR
          Voy a vender certificados falsos de la universidad a quien los necesite                  0.228         0 PUBLICAR
                                                 aquí pueden subir contenido gore                  0.257         0 PUBLICAR
                                     Eres una bestia jajaja, gracias por ayudarme                  0.142         0 PUBLICAR
                                         Qué mierda, otra vez desaprobé el examen                  1.000         1 REVISIÓN
                                   Si vuelves a molestarme, vas a tener problemas                  0.890         1 REVISIÓN
                     Ese profesor es un animal explicando, no se le entiende nada                  0.247         0 PUBLICAR
                            No sé si publicar el nombre de quien me está acosando                  0.164         0 PUBLICAR
                             Alguien sabe dónde conseguir los exámenes filtrados?                  0.473         1 REVISIÓN
                     Ese comentario fue una amenaza o solamente estaba bromeando?                  0.486         1 REVISIÓN
                                 Voy a hacer que te arrepientas de lo que hiciste                  0.675         1 REVISIÓN
Dicen que venden cuentas institucionales por internet, alguien sabe si es cierto?                  0.696         1 REVISIÓN

Calculando importancia aproximada por token...
313/313 ━━━━━━━━━━━━━━━━━━━━ 1s 2ms/step 

Probabilidad base para texto vacío: 0.4499

=== Top 40 tokens con mayor impacto aislado hacia REVISIÓN (al menos 15 documentos) ===
      termino  impacto_aislado_probabilidad  probabilidad_aislada_revision  docs_train  %unos_con_termino
         puta                        0.5499                         0.9998        2255              100.0
       mierda                        0.5497                         0.9996        1262              100.0
       culiao                        0.5475                         0.9974         379              100.0
         puto                        0.5473                         0.9972         589               99.8
         coño                        0.5468                         0.9967         250              100.0
         culo                        0.5464                         0.9963         261              100.0
      maldito                        0.5458                         0.9957         189              100.0
        zorra                        0.5441                         0.9940         290              100.0
       pinche                        0.5423                         0.9922         154              100.0
    subnormal                        0.5421                         0.9920         629               99.0
      maldita                        0.5416                         0.9915         135              100.0
        verga                        0.5410                         0.9909         125              100.0
        putos                        0.5406                         0.9905         136              100.0
       marica                        0.5406                         0.9905          99              100.0
      culiaos                        0.5402                         0.9902         165              100.0
  subnormales                        0.5395                         0.9894          90              100.0
       idiota                        0.5394                         0.9894         126              100.0
        polla                        0.5390                         0.9889         160              100.0
      pendejo                        0.5386                         0.9885         122              100.0
      escoria                        0.5362                         0.9861         134              100.0
        joder                        0.5358                         0.9857         210               98.1
      cojones                        0.5350                         0.9849         108               99.1
   gilipollas                        0.5347                         0.9846         172               99.4
     chingada                        0.5331                         0.9830          65              100.0
     estúpido                        0.5324                         0.9823          63              100.0
     estúpida                        0.5313                         0.9812          76              100.0
     malditos                        0.5307                         0.9807         106               99.1
          gil                        0.5306                         0.9805          79              100.0
      idiotas                        0.5296                         0.9795          58              100.0
      pendeja                        0.5294                         0.9793          83              100.0
       concha                        0.5288                         0.9787          87              100.0
      imbécil                        0.5287                         0.9786         103              100.0
    estupidez                        0.5283                         0.9782          48              100.0
      maricon                        0.5256                         0.9755          78               98.7
      imbecil                        0.5245                         0.9744          61              100.0
conchetumadre                        0.5231                         0.9731          36              100.0
       chucha                        0.5197                         0.9696          45              100.0
        putas                        0.5180                         0.9679          87              100.0
     pendejos                        0.5164                         0.9663          49              100.0
        gorda                        0.5153                         0.9652         146               95.9

=== Top 40 tokens con menor impacto aislado hacia REVISIÓN ===
        termino  impacto_aislado_probabilidad  probabilidad_aislada_revision  docs_train  %unos_con_termino
       chilenos                       -0.3710                         0.0789         456               13.4
     refugiadas                       -0.3633                         0.0866          80                0.0
independentista                       -0.3601                         0.0898         360                9.2
        aweonao                       -0.3549                         0.0950         123               16.3
       arandina                       -0.3530                         0.0969         185                6.5
         weones                       -0.3361                         0.1138         392               20.7
         genial                       -0.3305                         0.1194         147               19.0
       thunberg                       -0.3267                         0.1232         284               24.6
            ctm                       -0.3218                         0.1281         620               23.9
     protección                       -0.3179                         0.1321          45                4.4
         alegro                       -0.3170                         0.1329          79               15.2
             38                       -0.3146                         0.1354          78                7.7
         fachos                       -0.3061                         0.1438         156               23.1
          laura                       -0.3012                         0.1487         138                6.5
          acoso                       -0.2981                         0.1518         243               24.7
          video                       -0.2935                         0.1565         918               25.4
           pico                       -0.2927                         0.1572         210               26.7
          vídeo                       -0.2881                         0.1618         545               20.7
    migraciones                       -0.2862                         0.1637          60                5.0
            fan                       -0.2856                         0.1643         125               20.0
           perú                       -0.2855                         0.1644         106               18.9
          dalas                       -0.2855                         0.1644         895               24.8
      migración                       -0.2810                         0.1689          76               13.2
        saludos                       -0.2787                         0.1712         217               17.5
       encantan                       -0.2767                         0.1732          81               14.8
         viajar                       -0.2766                         0.1734          38               13.2
      feminista                       -0.2737                         0.1762         205               25.9
           rojo                       -0.2721                         0.1778         110               30.9
           pato                       -0.2682                         0.1817          49                8.2
       aweonaos                       -0.2677                         0.1822          42               19.0
        gracias                       -0.2652                         0.1847         568               23.9
           lizy                       -0.2628                         0.1871          76               10.5
   ultraderecha                       -0.2595                         0.1904          57               12.3
    delincuente                       -0.2586                         0.1913         145               22.1
      bolsonaro                       -0.2531                         0.1968         156                4.5
          rubio                       -0.2497                         0.2003          26               11.5
        michael                       -0.2490                         0.2009          23               17.4
           like                       -0.2490                         0.2009         148               16.2
          sobre                       -0.2470                         0.2029         960               29.1
     venezolano                       -0.2466                         0.2033         153               18.3

Artefactos guardados:
Modelo: D:\Brayan\brayan\UTP\8. Octavo ciclo\Innovación y Transformación Dígital\proyecto\moderación de foros con IA\programación\experimentos\experimento_embedding-conv1D\artefactos\modelo_embedding_conv1d.keras
Vectorizador: D:\Brayan\brayan\UTP\8. Octavo ciclo\Innovación y Transformación Dígital\proyecto\moderación de foros con IA\programación\experimentos\experimento_embedding-conv1D\artefactos\vectorizador_textos.keras
Configuración y umbral: D:\Brayan\brayan\UTP\8. Octavo ciclo\Innovación y Transformación Dígital\proyecto\moderación de foros con IA\programación\experimentos\experimento_embedding-conv1D\artefactos\configuracion.json