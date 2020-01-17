function usage() 
{
    echo " Usage : "
    echo "   bash asset_run.sh deploy"
    echo "   bash asset_run.sh start "
    echo " "
    echo " "
    echo "examples : "
    echo "   bash asset_run.sh deploy "
    echo "   bash asset_run.sh start  "
    exit 0
}

    case $1 in
    deploy)
            [ $# -lt 1 ] && { usage; }
            ;;
    start)
            [ $# -lt 1 ] && { usage; }
            ;;
    register)
            [ $# -lt 3 ] && { usage; }
            ;;
    transfer)
            [ $# -lt 4 ] && { usage; }
            ;;
    query)
            [ $# -lt 2 ] && { usage; }
            ;;
    add)
            [ $# -lt 2 ] && { usage; }
            ;;
    takeAccount)
            [ $# -lt 2 ] && { usage; }
            ;;
    removeAccount)
            [ $# -lt 1 ] && { usage; }
            ;;
    *)
        usage
            ;;
    esac

    java -Djdk.tls.namedGroups="secp256k1" -cp 'apps/*:conf/:lib/*' org.fisco.bcos.asset.client.AssetClient $@

