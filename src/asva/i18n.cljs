(ns asva.i18n
  (:require
   [clojure.string :as str]
   [tongue.core :as tongue]))

(def ^:private format-number-en
  (tongue/number-formatter {:group "," :decimal "."}))

(def ^:private inst-strings-en
  {:weekdays-narrow ["S" "M" "T" "W" "T" "F" "S"]
   :weekdays-short  ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
   :weekdays-long   ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
   :months-long     ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["BC" "AD"]
   :eras-long       ["Before Christ" "Anno Domini"]})

(def ^:private inst-strings-pt
  {:weekdays-narrow ["D" "S" "T" "Q" "Q" "S" "S"]
   :weekdays-short  ["Dom" "Seg" "Ter" "Qua" "Qui" "Sex" "Sáb"]
   :weekdays-long   ["Domingo" "Segunda-feira" "Terça-feira" "Quarta-feira" "Quinta-feira" "Sexta-feira" "Sábado"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Fev" "Mar" "Abr" "Mai" "Jun" "Jul" "Ago" "Set" "Out" "Nov" "Dez"]
   :months-long     ["Janeiro" "Fevereiro" "Março" "Abril" "Maio" "Junho" "Julho" "Agosto" "Setembro" "Outubro" "Novembro" "Dezembro"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["AC" "DC"]
   :eras-long       ["Antes de Cristo" "Depois de Cristo"]})

(def ^:private inst-strings-es
  {:weekdays-narrow ["D" "L" "M" "M" "J" "V" "S"]
   :weekdays-short  ["Dom" "Lun" "Mar" "Mié" "Jue" "Vie" "Sáb"]
   :weekdays-long   ["Domingo" "Lunes" "Martes" "Miércoles" "Jueves" "Viernes" "Sábado"]
   :months-narrow   ["E" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Ene" "Feb" "Mar" "Abr" "May" "Jun" "Jul" "Ago" "Sep" "Oct" "Nov" "Dic"]
   :months-long     ["Enero" "Febrero" "Marzo" "Abril" "Mayo" "Junio" "Julio" "Agosto" "Septiembre" "Octubre" "Noviembre" "Diciembre"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["AC" "DC"]
   :eras-long       ["Antes de Cristo" "Después de Cristo"]})

(def ^:private inst-strings-nl
  {:weekdays-narrow ["Z" "M" "D" "W" "D" "V" "Z"]
   :weekdays-short  ["Zon" "Maan" "Dins" "Woen" "Don" "Vrij" "Zat"]
   :weekdays-long   ["Zondag" "Maandag" "Dinsdag" "Woensdag" "Donderdag" "Vrijdag" "Zaterdag"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mrt" "Apr" "Mei" "Jun" "Jul" "Aug" "Sep" "Okt" "Nov" "Dec"]
   :months-long     ["Januari" "Februari" "Maart" "April" "Mei" "Juni" "Juli" "Augustus" "September" "Oktober" "November" "December"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["v.Chr." "n.Chr."]
   :eras-long       ["Voor Christus" "Na Christus"]})

(def ^:private inst-strings-ko
  {:weekdays-narrow ["일" "월" "화" "수" "목" "금" "토"]
   :weekdays-short  ["일요일" "월요일" "화요일" "수요일" "목요일" "금요일" "토요일"]
   :weekdays-long   ["일요일" "월요일" "화요일" "수요일" "목요일" "금요일" "토요일"]
   :months-narrow   ["1" "2" "3" "4" "5" "6" "7" "8" "9" "10" "11" "12"]
   :months-short    ["1월" "2월" "3월" "4월" "5월" "6월" "7월" "8월" "9월" "10월" "11월" "12월"]
   :months-long     ["1월" "2월" "3월" "4월" "5월" "6월" "7월" "8월" "9월" "10월" "11월" "12월"]
   :dayperiods      ["오전" "오후"]
   :eras-short      ["기원전" "기원후"]
   :eras-long       ["기원전" "기원후"]})

(def ^:private inst-strings-no
  {:weekdays-narrow ["S" "M" "T" "O" "T" "F" "L"]
   :weekdays-short  ["Søn" "Man" "Tir" "Ons" "Tor" "Fre" "Lør"]
   :weekdays-long   ["Søndag" "Mandag" "Tirsdag" "Onsdag" "Torsdag" "Fredag" "Lørdag"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "Mai" "Jun" "Jul" "Aug" "Sep" "Okt" "Nov" "Des"]
   :months-long     ["Januar" "Februar" "Mars" "April" "Mai" "Juni" "Juli" "August" "September" "Oktober" "November" "Desember"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["f.Kr." "e.Kr."]
   :eras-long       ["Før Kristus" "Etter Kristus"]})

(def ^:private is-finite? js/isFinite)

(defn ^:private round-to-decimals
  [n decimals]
  (if (is-finite? n)
    (let [factor (Math/pow 10 decimals)]
      (/ (Math/round (* n factor)) factor))
    0))

(def dicts
  {:en
   {:tongue/format-number format-number-en
    :tongue/format-inst (tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en)
    :date (tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en)
    :date-time (tongue/inst-formatter "{month-short} {day}, {year} {hour24}:{minutes-padded}" inst-strings-en)
    :12-hour (tongue/inst-formatter "{hour12}" inst-strings-en)
    :two-decimals (fn [num] (format-number-en (round-to-decimals num 2)))
    :percent (fn [num] (str (format-number-en (round-to-decimals num 2)) "%"))
    :last-updated (fn [d] (str "Last updated " ((tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en) d)))

    :main-title  "Welcome to Our ASVA Assessment Page"
    :preface-1 "Our mission is to integrate robust security practices into our services. This assessment, aligned with "
    :preface-link-1 "OWASP's ASVA guidelines"
    :preface-2 ", ensures our applications are secure and resilient. We leverage resources like the "
    :preface-link-2 "OWASP Top Ten Proactive Controls"
    :preface-3 " to guide our approach, fostering a culture of security and awareness. Your expertise and collaboration in this process are key to maintaining our commitment to security excellence."

    :assessment-notes "Notes on compliance, risks, and mitigation strategies"
    :export-assessments "Export the assessments as a JSON-file"
    :import-assessments "Import an assessments JSON-file"
    :markdown-support "This textarea supports markdown formatting"

    :file-upload-info "Drag & drop or click to choose file to upload"
    :file-upload "ASVS JSON (.json, Max: 1 MB)"

    :search "Search..."
    :copy-location "Copy location of anchor to your clipboard"
    :level "Level {1}"
    :not-applicable "Not applicable"
    :completed "Completed"

    :architecture "Architecture"
    :authentication "Authentication"
    :session-management "Session management"
    :access-control "Access control"
    :validation-sanitization-encoding "Validation Sanitization Encoding"
    :cryptography "Cryptography"
    :error-logging "Error logging"
    :data-protection "Data protection"
    :communications "Communications"
    :malicious "Malicious"
    :business-logic "Business logic"
    :files-resources "Files & resources"
    :api "API"
    :config "Config"

    :failed-parsing "Error parsing the ASVS file. Ensure the file is in correct JSON format and check for syntax errors."
    :failed-reading "Error reading the ASVS file. Check if the file exists, is accessible, and is in JSON format."}

   :nb
   {:last-updated (fn [d] (str "Sist oppdatert " ((tongue/inst-formatter "{day}. {month-long} {year}" inst-strings-no) d)))
    :main-title "Velkommen til vår ASVA-vurderingsside"
    :preface-1 "Vår misjon er å integrere robuste sikkerhetspraksiser i våre tjenester. Denne vurderingen, i tråd med "
    :preface-link-1 "OWASPs ASVA-retningslinjer"
    :preface-2 ", sikrer at våre applikasjoner er sikre og motstandsdyktige. Vi benytter ressurser som "
    :preface-link-2 "OWASPs topp ti proaktive kontroller"
    :preface-3 " for å lede vår tilnærming, og fremmer en kultur for sikkerhet og bevissthet. Din ekspertise og samarbeid i denne prosessen er nøkkelen til å opprettholde vårt engasjement for sikkerhetseksellens."

    :file-upload-info "Dra & slipp eller klikk for å velge fil for opplasting"
    :file-upload "ASVS JSON (.json, Maks: 1 MB)"

    :failed-parsing "Feil ved parsing av ASVS-filen. Sørg for at filen er i korrekt JSON-format og sjekk for syntaksfeil."
    :failed-reading "Feil ved lesing av ASVS-filen. Kontroller at filen eksisterer, er tilgjengelig, og er i JSON-format."

    :search "Søk..."
    :copy-location "Kopier plassering av anker til utklippstavlen din"
    :level "Nivå {1}"
    :not-applicable "Ikke relevant"
    :completed "Fullført"

    :architecture "Arkitektur"
    :authentication "Autentisering"
    :session-management "Sesjonsstyring"
    :access-control "Tilgangskontroll"
    :validation-sanitization-encoding "Validering, Sanitering, Koding"
    :cryptography "Kryptografi"
    :error-logging "Feillogging"
    :data-protection "Databeskyttelse"
    :communications "Kommunikasjon"
    :malicious "Skadelig"
    :business-logic "Forretningslogikk"
    :files-resources "Filer & ressurser"
    :api "API"
    :config "Konfigurasjon"}


   :nl
   {:last-updated (fn [d] (str "Laatst bijgewerkt " ((tongue/inst-formatter "{day} {month-long} {year}" inst-strings-nl) d)))
    :main-title "Welkom op onze ASVA-beoordelingspagina"
    :preface-1 "Onze missie is om robuuste beveiligingspraktijken te integreren in onze services. Deze beoordeling, in lijn met "
    :preface-link-1 "OWASP's ASVA-richtlijnen"
    :preface-2 ", zorgt ervoor dat onze applicaties veilig en veerkrachtig zijn. We gebruiken bronnen zoals de "
    :preface-link-2 "OWASP Top Tien Proactieve Controles"
    :preface-3 " om onze aanpak te sturen, en cultiveren een cultuur van veiligheid en bewustzijn. Uw expertise en samenwerking in dit proces zijn essentieel om onze toewijding aan veiligheidsexcellentie te behouden."
    :search "Zoeken..."
    :copy-location "Kopieer de locatie van de anker naar uw klembord"
    :level "Niveau {1}"
    :file-upload-info "Sleep & zet neer of klik om bestand te kiezen om te uploaden"
    :file-upload "ASVS JSON (.json, Max: 1 MB)"
    :not-applicable "Niet van toepassing"
    :completed "Voltooid"
    :architecture "Architectuur"
    :authentication "Authenticatie"
    :session-management "Sessiebeheer"
    :access-control "Toegangscontrole"
    :validation-sanitization-encoding "Validatie Sanitisatie Encoding"
    :cryptography "Cryptografie"
    :error-logging "Foutenlogboek"
    :data-protection "Gegevensbescherming"
    :failed-parsing "Fout bij het parsen van het ASVS-bestand. Zorg ervoor dat het bestand in het juiste JSON-formaat is en controleer op syntaxisfouten."
    :failed-reading "Fout bij het lezen van het ASVS-bestand. Controleer of het bestand bestaat, toegankelijk is en in JSON-formaat is."
    :communications "Communicatie"
    :malicious "Kwaadaardig"
    :business-logic "Bedrijfslogica"
    :files-resources "Bestanden & bronnen"
    :api "API"
    :config "Configuratie"}

   :pt
   {:last-updated (fn [d] (str "Última atualização " ((tongue/inst-formatter "{day} de {month-long} de {year}" inst-strings-pt) d)))
    :main-title "Bem-vindo à nossa Página de Avaliação ASVA"
    :preface-1 "Nossa missão é integrar práticas de segurança robustas aos nossos serviços. Esta avaliação, alinhada com "
    :preface-link-1 "as diretrizes da ASVA da OWASP"
    :preface-2 ", garante que nossas aplicações sejam seguras e resilientes. Utilizamos recursos como o "
    :preface-link-2 "OWASP Top Dez Controles Proativos"
    :preface-3 " para orientar nossa abordagem, promovendo uma cultura de segurança e consciência. Sua expertise e colaboração neste processo são fundamentais para manter nosso compromisso com a excelência em segurança."
    :search "Pesquisar..."
    :copy-location "Copie a localização da âncora para sua área de transferência"
    :level "Nível {1}"
    :file-upload-info "Arraste e solte ou clique para escolher o arquivo para upload"
    :file-upload "ASVS JSON (.json, Máx: 1 MB)"
    :not-applicable "Não aplicável"
    :failed-parsing "Erro ao fazer o parse do arquivo ASVS. Certifique-se de que o arquivo está no formato JSON correto e verifique se há erros de sintaxe."
    :failed-reading "Erro ao ler o arquivo ASVS. Verifique se o arquivo existe, está acessível e está em formato JSON."
    :completed "Concluído"
    :architecture "Arquitetura"
    :authentication "Autenticação"
    :session-management "Gestão de sessões"
    :access-control "Controle de acesso"
    :validation-sanitization-encoding "Validação Sanitização Codificação"
    :cryptography "Criptografia"
    :error-logging "Registro de Erros"
    :data-protection "Proteção de Dados"
    :communications "Comunicações"
    :malicious "Malicioso"
    :business-logic "Lógica de Negócios"
    :files-resources "Arquivos & recursos"
    :api "API"
    :config "Configuração"}

   :es
   {:last-updated (fn [d] (str "Última actualización " ((tongue/inst-formatter "{day} de {month-long} de {year}" inst-strings-es) d)))
    :main-title "Bienvenido a nuestra página de evaluación ASVA"
    :preface-1 "Nuestra misión es integrar prácticas de seguridad robustas en nuestros servicios. Esta evaluación, alineada con "
    :preface-link-1 "las pautas de ASVA de OWASP"
    :preface-2 ", asegura que nuestras aplicaciones sean seguras y resilientes. Utilizamos recursos como "
    :preface-link-2 "OWASP Top Ten Controles Proactivos"
    :preface-3 " para guiar nuestro enfoque, fomentando una cultura de seguridad y conciencia. Su experiencia y colaboración en este proceso son clave para mantener nuestro compromiso con la excelencia en seguridad."
    :search "Buscar..."
    :copy-location "Copiar la ubicación del ancla a su portapapeles"
    :level "Nivel {1}"
    :not-applicable "No aplicable"
    :file-upload-info "Arrastra y suelta o haz clic para elegir el archivo a subir"
    :file-upload "ASVS JSON (.json, Máx: 1 MB)"
    :completed "Completado"
    :architecture "Arquitectura"
    :authentication "Autenticación"
    :session-management "Gestión de sesiones"
    :access-control "Control de acceso"
    :failed-parsing "Error al analizar el archivo ASVS. Asegúrate de que el archivo esté en el formato JSON correcto y revisa si hay errores de sintaxis."
    :failed-reading "Error al leer el archivo ASVS. Comprueba si el archivo existe, está accesible y está en formato JSON."
    :validation-sanitization-encoding "Validación Saneamiento Codificación"
    :cryptography "Criptografía"
    :error-logging "Registro de errores"
    :data-protection "Protección de datos"
    :communications "Comunicaciones"
    :malicious "Malicioso"
    :business-logic "Lógica empresarial"
    :files-resources "Archivos & recursos"
    :api "API"
    :config "Configuración"}

   :ko
   {:last-updated (fn [d] (str "마지막 업데이트 " ((tongue/inst-formatter "{year}년 {month-long} {day}일" inst-strings-ko) d)))
    :main-title "우리 ASVA 평가 페이지에 오신 것을 환영합니다"
    :preface-1 "우리의 사명은 우리 서비스에 강력한 보안 관행을 통합하는 것입니다. 이 평가는"
    :preface-link-1 "OWASP의 ASVA 가이드라인"
    :preface-2 "에 부합하여, 우리의 애플리케이션이 안전하고 복원력이 있도록 보장합니다. 우리는 "
    :preface-link-2 "OWASP 상위 십 개의 적극적인 제어"
    :preface-3 "와 같은 자원을 활용하여 접근 방식을 안내하고, 보안과 인식의 문화를 조성합니다. 이 과정에서의 귀하의 전문 지식과 협력은 우리의 보안 우수성에 대한 약속을 유지하는 데 중요합니다."
    :search "검색..."
    :copy-location "앵커의 위치를 클립보드에 복사"
    :file-upload-info "드래그 & 드롭 또는 업로드할 파일을 선택하려면 클릭하세요"
    :file-upload "ASVS JSON (.json, 최대: 1 MB)"
    :level "수준 {1}"
    :not-applicable "해당 없음"
    :completed "완료됨"
    :architecture "건축"
    :authentication "인증"
    :session-management "세션 관리"
    :access-control "접근 제어"
    :validation-sanitization-encoding "검증 살균 인코딩"
    :failed-parsing "ASVS 파일 파싱 오류. 파일이 올바른 JSON 형식인지 확인하고 구문 오류를 확인하세요."
    :failed-reading "ASVS 파일 읽기 오류. 파일이 존재하는지, 접근 가능한지, JSON 형식인지 확인하세요."
    :cryptography "암호화"
    :error-logging "오류 로깅"
    :data-protection "데이터 보호"
    :communications "통신"
    :malicious "악의적인"
    :business-logic "비즈니스 로직"
    :files-resources "파일 & 자원"
    :api "API"
    :config "구성"}

   :tongue/fallback :en})

(def ^:private translate
  (tongue/build-translate dicts))

(defn t [& args]
  (let [[dialect _] (-> js/navigator .-language (str/split "-"))]
    (apply translate (-> dialect str/lower-case keyword) args)))