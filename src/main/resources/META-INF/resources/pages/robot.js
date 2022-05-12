export default {
    name: 'Robot',
    setup() {
        const title = '123'
        return {title}
    },
    data() {
        return {
            isRun: false,
            logs: [],
            eb: {},
            exchanges: []
        }
    },
    methods: {
        onStart() {
            axios.get("/strategy/start")
                .then(response => {
                    console.log(response);
                    this.isRun = true;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onStop() {
            axios.get("/strategy/stop")
                .then(response => {
                    console.log(response);
                    this.isRun = false;
                })
                .catch(e => {
                    console.info(e);
                });
        }
    },
    mounted() {
        this.eb = new EventBus('/eventbus');
        this.eb.onopen = () => {
            console.log(">>> Open Event bus");
            this.eb.registerHandler('LOG', (error, message) => {
                this.logs.push(message.body);
            })
        }

        axios.get("/account/exchanges")
            .then(response => {
                this.exchanges = response.data;
            })
    }
    ,

    template: `
      
        <h1>{{title}}</h1>
        <div class="grid">
            <div class="col-3">Биржи:</div>
            <div class="col-9">
                <div v-for="(exc, index) in exchanges" >
                    <b style="padding-right: 10px">{{exc.name}}</b> 
                    <span v-if="exc.open">открыта</span>
                    <span v-else>закрыта, 
                        <span v-if="exc.tradingDay">откроется через {{exc.hoursBeforeOpen}} часов {{exc.minutesBeforeOpen}} минут</span>
                        <span v-else>выходной</span>
                    </span>
                    <br/>
                </div>
            </div>
        </div>  
        
        <button @click="onStart" >Go</button>
        <button @click="onStop" v-if="isRun" >Stop</button>
        
        <div class="log-panel">
            <div v-for="(log, index) in logs" >
                {{logs[index]}}
            </div>
        </div>
        

      
    `
}