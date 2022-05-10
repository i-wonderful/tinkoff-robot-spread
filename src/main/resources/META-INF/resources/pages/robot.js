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
            eb: {}
        }
    },
    methods: {
        onStart() {
            axios.get("/robot/start")
                .then(response => {
                    console.log(response);
                    this.isRun = true;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onStop() {
            // todo
            this.isRun = false;
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
    }
    ,

    template: `
      <div>
        <h1>{{title}}</h1>
        
        <div v-for="(log, index) in logs">
            <span>{{logs[index]}}</span><br/>
        </div>
        
        <p id="log"></p>
        
        <button @click="onStart">Go</button>
        <button v-if="isRun">Stop</button>
      </div>
    `
}